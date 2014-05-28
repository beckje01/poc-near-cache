import poc.service.RestService
import ratpack.exec.SuccessPromise
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse
import rx.functions.Action1

import java.util.concurrent.CountDownLatch

import static ratpack.groovy.Groovy.ratpack

ratpack {
	handlers { RestService restService ->
		get {

			def url = request.queryParams.get("url")
			//			if (url) {

			def result = restService.getResource(url)
			result.single().subscribe {
				println "here " + it.statusCode
				context.render "Got: "+it.statusCode
			}
			result.doOnError(new Action1<Throwable>() {
				@Override
				void call(Throwable throwable) {
					context.render "Error happened ${throwable}"
				}
			})

			//			} else {
			//				render "Please provide a url"
			//			}

		}

		assets "public"
	}
}
