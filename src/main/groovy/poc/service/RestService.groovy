package poc.service

import com.google.inject.Inject
import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixObservableCommand
import net.sf.ehcache.Cache
import net.sf.ehcache.Element
import net.sf.ehcache.config.CacheConfiguration
import net.sf.ehcache.config.Configuration
import ratpack.http.client.HttpClient
import ratpack.http.client.HttpClients
import ratpack.http.client.ReceivedResponse
import ratpack.http.client.internal.DefaultHttpClient
import ratpack.launch.LaunchConfig
import ratpack.launch.LaunchConfigs
import rx.Observable
import rx.functions.Action1

import static ratpack.rx.RxRatpack.observe

import net.sf.ehcache.CacheManager
import java.util.concurrent.CountDownLatch

/**
 * Created by jeff.beck on 5/26/14.
 */
class RestService {

	private final LaunchConfig launchConfig

	private Cache restCache

	@Inject
	public RestService(LaunchConfig launchConfig) {
		this.launchConfig = launchConfig



		def cacheManager = CacheManager.getInstance()

		restCache = cacheManager.getCache("restCache")
		if (!restCache) {
			def cacheConfig = new CacheConfiguration("restCache", 0)
			cacheConfig.transactionalMode(CacheConfiguration.TransactionalMode.OFF)

			cacheManager.addCache(new Cache(cacheConfig))
			restCache = cacheManager.getCache("restCache")
		}
	}

	Observable<ReceivedResponse> getResource(String url) {
		println "getResource $url"

		def cachedResource = restCache.get(url)
		if (cachedResource) {
			println "Found Cached Item created: ${cachedResource.creationTime}"
			return Observable.from((ReceivedResponse)cachedResource.getObjectValue())
		} else {
			println "Cache Miss"
			def client = HttpClients.httpClient(launchConfig)

			def observableResponseCommand = new HystrixObservableCommand<ReceivedResponse>(HystrixCommandGroupKey.Factory.asKey("http-get")) {

				@Override
				protected Observable<ReceivedResponse> run() {

					def observable = observe(client.get(url))

					return observable
				}

			}
			println observableResponseCommand.isCircuitBreakerOpen()


			def observableResponse = observableResponseCommand.toObservable()


			//This correctly throws an error
			//def observableResponse = observe(client.get(url))

			//Cache if we should
			observableResponse.subscribe({ ReceivedResponse receivedResponse ->
				def cacheControl = parseCacheHeader(receivedResponse.headers.get("Cache-Control"))
				println cacheControl
				def maxAge = cacheControl['max-age'] ?: 0
				maxAge=30
				if (!cacheControl['no-cache'] && maxAge > 0) {
					println "Caching"

					Element elm = new Element(url, receivedResponse)
					elm.timeToLive = maxAge
					restCache.put(elm)
				}
			},(Action1<Throwable>){Throwable throwable ->
				println throwable
			})


			return observableResponse
		}
	}

	private def parseCacheHeader(String cacheHeader) {

		def parts = cacheHeader.split(',')
		def cacheControl = [:]
		parts.each {
			def keyValue = it.split('=')
			if (keyValue.size() == 1) {
				cacheControl[keyValue[0].toLowerCase()] = ""
			} else if (keyValue.size() == 2) {
				cacheControl[keyValue[0].toLowerCase()] = keyValue[1]
			} else {
				println "Broken: " + keyValue
			}
		}

		return cacheControl
	}
}

//		observableResponse.subscribe({
//			println it.class
//			return it.statusCode + ""
//		})

//		response.onError({
//			out = "Error"
//		})
//		response.then({ ReceivedResponse receivedResponse ->
//			out = receivedResponse.statusCode
//		})