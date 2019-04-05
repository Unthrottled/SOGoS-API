package io.acari.http

import io.acari.util.toOptional
import io.reactivex.Maybe
import io.reactivex.functions.BiFunction
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.ext.web.Router

object UserService {
  fun createUser(accessToken: AccessToken): Maybe<String> =
    Maybe.just(accessToken.accessToken())
      .zipWith(
        Maybe.just(accessToken.idToken()),
        BiFunction<JsonObject, JsonObject, Pair<JsonObject, JsonObject>> { t1, t2 -> Pair(t1, t2) })
      .map {
        val idToken = it.second
        JsonObject()
          .put("fullName", idToken.getValue("name"))
          .put("userName", idToken.getValue("preferred_username"))
          .put("firstName", idToken.getValue("given_name"))
          .put("lastName", idToken.getValue("family_name"))
          .put("email", idToken.getValue("email"))
          .encodePrettily()
      }

}

fun mountAPIRoute(router: Router): Router =
  router.toOptional()
    .map {
      it.get("/user")
        .handler { request ->
          val accessToken = request.user() as AccessToken
          UserService.createUser(accessToken)
            .subscribe({
              request.response()
                .putHeader("content-type", "application/json")
                .end(it)
            }) {
              request.fail(404)
            }
        }

      it.get("/")
        .handler { req ->
          val user = req.user() as AccessToken
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
      it
    }.orElse(router)
