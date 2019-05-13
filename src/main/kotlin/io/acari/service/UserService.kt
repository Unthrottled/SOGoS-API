package io.acari.service

import com.google.common.hash.Hashing
import io.acari.util.loggerFor
import io.acari.util.toOptional
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.reactivex.MaybeHelper
import java.lang.IllegalStateException
import java.util.*

object UserService {

  private val log = loggerFor(javaClass)

  fun findUserInformation(user: User): Maybe<String> =
      Single.just(user)
        .filter { it is AccessToken }
        .map { it as AccessToken }
        .flatMap {     accessToken ->
          extractOAuthUserInformation(accessToken)
        }
        .map { extractUser(it) }
        .switchIfEmpty(Maybe.error{ IllegalStateException("Unable to create user profile from user: $user") })

  private fun extractOAuthUserInformation(accessTokenAndStuff: AccessToken): Maybe<JsonObject> {
    val accessToken = accessTokenAndStuff.accessToken()
    return Maybe.just(accessToken)
      .flatMap {
        accessTokenAndStuff.idToken().toOptional()
          .map { Maybe.just(it) }
          .orElseGet {
            MaybeHelper.toMaybe {
              accessTokenAndStuff.userInfo(it)
            }
          }
      }.onErrorReturn {
        log.warn("Unable to fetch user info, falling back to access token", it)
        accessToken
      }
  }

  val hashingFunction = Hashing.sha256()

  private fun extractUser(idToken: JsonObject): String {
    return JsonObject()
      .put("fullName", idToken.getValue("name"))
      .put("userName", idToken.getValue("preferred_username"))
      .put("firstName", idToken.getValue("given_name"))
      .put("lastName", idToken.getValue("family_name"))
      .put("email", idToken.getValue("email"))
      .put("key", hashingFunction.hashString(idToken.getString("email"), Charsets.UTF_16).toString())
      .put("guid", UUID.randomUUID().toString())
      .encode()
  }
}
