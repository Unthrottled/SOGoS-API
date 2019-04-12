package io.acari.developer

import io.acari.util.toOptional
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Single
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import java.util.*

fun createStaticContentProxy(
  vertx: Vertx,
  configuration: JsonObject
): Optional<Handler<RoutingContext>> {
  val devServerConfigurations = configuration.getJsonObject("server").getJsonObject("dev-server")
  val isDev = devServerConfigurations != null
  return isDev.toOptional()
    .filter { it }
    .map {
      val proxy = vertx.createHttpClient()
      val hostToProxy = devServerConfigurations.getString("host")
      val hostPortToProxy = devServerConfigurations.getInteger("port")
      Handler<RoutingContext> { request ->
        val requestToProxy = request.request()
        val response = request.response()
        Single.create<HttpClientResponse> { sink ->
          proxy.request(requestToProxy.method(), hostPortToProxy, hostToProxy, requestToProxy.uri())
          { response ->
            sink.onSuccess(response)
          }.end()
        }.flatMapPublisher { httpClientResponse ->
          response.isChunked = true
          response.statusCode = httpClientResponse.statusCode()
          response.headers().setAll(httpClientResponse.headers())
          Flowable.create<Buffer>({ flowSink ->
            httpClientResponse.handler { data ->
              flowSink.onNext(data)
            }
            // todo: error handling?
            httpClientResponse.endHandler {
              flowSink.onComplete()
            }
          }, BackpressureStrategy.BUFFER)
        }.subscribe({
          response.write(it)
        }, {
          response.setStatusCode(404).end()
        }) {
          response.end()
        }
      }
    }
}
