package io.acari.http

import io.acari.util.toOptional
import io.reactivex.Maybe
import io.reactivex.functions.BiFunction
import io.vertx.codegen.annotations.Nullable
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.ext.auth.oauth2.impl.OAuth2TokenImpl
import io.vertx.ext.web.Router
import io.vertx.reactivex.MaybeHelper
import io.vertx.reactivex.SingleHelper

object UserService {
  fun createUser(user: User): Maybe<String> =
      Maybe.just(user)
        .filter { it is AccessToken }
        .map { it as AccessToken }
        .flatMap {     accessToken ->
          createUserFromAccessToken(accessToken)
        }

  private fun createUserFromAccessToken(accessToken: AccessToken): Maybe<String>? {
    return Maybe.just(accessToken.accessToken())
      .zipWith(
        accessToken.idToken().toOptional()
          .map { Maybe.just(it) }
          .orElseGet {
            MaybeHelper.toMaybe {
              accessToken.userInfo(it)
            }
          },
        BiFunction<JsonObject, JsonObject, Pair<JsonObject, JsonObject>> { t1, t2 -> Pair(t1, t2) })
      .map {
        val idToken = it.second
        extractUser(idToken)
      }
  }

  private fun extractUser(idToken: JsonObject): String? {
    return JsonObject()
      .put("fullName", idToken.getValue("name"))
      .put("userName", idToken.getValue("preferred_username"))
      .put("firstName", idToken.getValue("given_name"))
      .put("lastName", idToken.getValue("family_name"))
      .put("email", idToken.getValue("email"))
      .encodePrettily()
  }
}

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
          it.printStackTrace()
          request.fail(404)
        }
    }
  return router
}
