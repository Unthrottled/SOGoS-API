package io.acari.http

import io.acari.util.toOptional
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.ext.web.Router
import io.vertx.reactivex.SingleHelper

fun mountAPIRoute(router: Router): Router =
  router.toOptional()
    .map {
      it.get("/user")
        .handler{ request ->
          val accessToken = request.user() as AccessToken
          SingleHelper.toSingle<JsonObject> {
            accessToken.userInfo(it) // todo: probably can use the id token
          }.subscribe({ userInfo ->
            request.response()
              .putHeader("content-type", "application/json")
              .end(userInfo.encodePrettily())
          }){
            request.fail(404)
          }
        }

      it.get("/")
        .handler { req ->
          val user = req.user() as AccessToken
          req.response()
            .putHeader("content-type", "text/plain")
            .end("""
                |Hello from Vert.x: ${user.idToken()}!
                |
                |${user.accessToken()}
                |
                |${user.refreshToken()}
            """.trimMargin())
        }
      it
    }.orElse(router)
