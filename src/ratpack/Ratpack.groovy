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

			def result = restService.getResource(url)
			result.single().subscribe({
				println "here " + it.statusCode
				context.render "Got: " + it.statusCode
			}, (Action1<Throwable>){
				println "error"
				context.render "Error"
			})

		}

		assets "public"
	}
}
