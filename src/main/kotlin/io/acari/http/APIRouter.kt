package io.acari.http

import io.acari.util.toOptional
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.ext.web.Router

fun mountAPIRoute(router: Router): Router =
  router.toOptional()
    .map {
      it.get("/")
        .handler { req ->
          val user = req.user() as AccessToken
          req.response()
            .putHeader("content-type", "text/plain")
            .end("""
                |Hello from Vert.x: ${user.idToken()}!
                |
                |${user.accessToken()}
            """.trimMargin())
        }
      it
    }.orElse(router)
