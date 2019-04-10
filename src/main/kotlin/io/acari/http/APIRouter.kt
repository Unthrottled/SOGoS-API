package io.acari.http

import io.acari.util.toOptional
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler

fun mountAPIRoute(vertx: Vertx, router: Router, configuration: JsonObject): Router {
  router.mountSubRouter("/api", createAPIRoute(vertx))

  router.get("/bruh").handler { context ->
    context.session().get<String>("foo").toOptional()
      .map { it.toOptional() }
      .orElse("Dunno".toOptional())
      .ifPresent {
        context.response().setStatusCode(200).end(it)
      }
  }

  router.get("/testo")
    .handler { req ->
      val user = req.user() as AccessToken
      req.session().put("foo", "bar")
      req.response()
        .putHeader("content-type", "text/plain")
        .end(
          """
                |Hello from Vert.x:
                |
                |${user.idToken().encodePrettily()}
                |
                |${user.accessToken().encodePrettily()}
                |
                |${user.refreshToken().encodePrettily()}
            """.trimMargin()
        )
    }


  // Static content path must be mounted last, as a fall back
  router.get("/*").handler(fetchStaticContentHandler(vertx, configuration))
    .failureHandler { routingContext -> routingContext.reroute("/") }

  return router
}

fun fetchStaticContentHandler(vertx: Vertx, configuration: JsonObject): Handler<RoutingContext> {
  val devServerConfigurations = configuration.getJsonObject("server").getJsonObject("dev-server")
  val isDev = devServerConfigurations != null
  return if (isDev) {
    val proxy = vertx.createHttpClient()
    val hostToProxy = devServerConfigurations.getString("host")
    val hostPortToProxy = devServerConfigurations.getInteger("port")
    Handler { request ->
      val proxyRequest = request.request()
      Single.create<HttpClientResponse> { sink ->
        proxy.request(proxyRequest.method(), hostPortToProxy, hostToProxy, proxyRequest.uri())
        { response ->
          sink.onSuccess(response)
        }
      }.subscribe({ proxyResponse ->
        val response = request.response()
        response.isChunked = true
        response.statusCode = proxyResponse.statusCode()
        response.headers().setAll(proxyResponse.headers())
        Flowable.create<Buffer>({ flowSink ->
          proxyResponse.handler { data ->
            flowSink.onNext(data)
          }
          // todo: error handling?
          proxyResponse.endHandler { flowSink.onComplete() }
        }, BackpressureStrategy.BUFFER)
          .subscribe({
            response.end()
          }) {
            response.setStatusCode(404).end()
          }
      }) {
        request.response().setStatusCode(404).end()
      }

    }
  } else {
    StaticHandler.create()
  }
}

fun createAPIRoute(vertx: Vertx): Router {
  val router = Router.router(vertx)
  router.get("/user")
    .handler { request ->
      Maybe.just("")// todo: authorization
        .flatMap { UserService.createUser(request.user()) }
        .switchIfEmpty(Maybe.error { IllegalAccessException() })
        .subscribe({
          request.response()
            .putHeader("content-type", "application/json")
            .end(it)
        }) {
          request.fail(404)
        }
    }
  return router
}
