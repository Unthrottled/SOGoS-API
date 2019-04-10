package io.acari.http

import io.acari.util.toOptional
import io.reactivex.Maybe
import io.reactivex.functions.BiFunction
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.reactivex.MaybeHelper

object UserService {
  fun createUser(user: User): Maybe<String> =
      Maybe.just(user)
        .filter { it is AccessToken }
        .map { it as AccessToken }
        .flatMap {     accessToken ->
          createUserFromAccessToken(accessToken)
        }

  private fun createUserFromAccessToken(accessToken: AccessToken): Maybe<String> {
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

  private fun extractUser(idToken: JsonObject): String {
    return JsonObject()
      .put("fullName", idToken.getValue("name"))
      .put("userName", idToken.getValue("preferred_username"))
      .put("firstName", idToken.getValue("given_name"))
      .put("lastName", idToken.getValue("family_name"))
      .put("email", idToken.getValue("email"))
      .encodePrettily()
  }
}
