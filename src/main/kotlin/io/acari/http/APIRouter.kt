package io.acari.http

import io.acari.util.toOptional
import io.reactivex.Maybe
import io.vertx.core.Vertx
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.ext.web.Router

fun mountAPIRoute(vertx: Vertx, router: Router): Router {
  router.mountSubRouter("/api", createAPIRoute(vertx))

  router.get("/bruh").handler { context ->
    context.session().get<String>("foo").toOptional()
      .map { it.toOptional() }
      .orElse("Dunno".toOptional())
      .ifPresent {
        context.response().setStatusCode(200).end(it)
      }
  }

  router.get("/")
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
  return router
}

fun createAPIRoute(vertx: Vertx): Router {
  val router = Router.router(vertx)
  router.get("/user")
    .handler { request ->
//      SingleHelper.toSingle<Boolean> {
//        request.user().isAuthorized("sogos:view-user", it)
//      }.filter { it }
        Maybe.just("")
        .flatMap { UserService.createUser(request.user()) }
        .switchIfEmpty(Maybe.error {IllegalAccessException()})
        .subscribe({
          request.response()
            .putHeader("content-type", "application/json")
            .end(it)
        }){
          request.fail(404)
        }
    }
  return router
}
