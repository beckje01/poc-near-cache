import poc.service.RestService
import ratpack.exec.SuccessPromise
import ratpack.http.client.HttpClient
import ratpack.http.client.ReceivedResponse

import java.util.concurrent.CountDownLatch

import static ratpack.groovy.Groovy.ratpack

ratpack {
	handlers { RestService restService ->
		get {

			def url = request.queryParams.get("url")
			//			if (url) {
			restService.getResource(url).single().subscribe {
				println "here " + it.statusCode
				context.render "Got: "+it.statusCode
			}

			//			} else {
			//				render "Please provide a url"
			//			}

		}

		assets "public"
	}
}
