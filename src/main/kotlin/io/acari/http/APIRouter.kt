package io.acari.http

import io.acari.developer.createStaticContentProxy
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
  router.get("/*")
    .handler(fetchStaticContentHandler(vertx, configuration))
    .failureHandler { routingContext -> routingContext.reroute("/") }

  return router
}

fun fetchStaticContentHandler(vertx: Vertx, configuration: JsonObject): Handler<RoutingContext> =
  createStaticContentProxy(vertx, configuration)
    .orElseGet {
    StaticHandler.create()
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
