package io.acari.service

import com.google.common.hash.Hashing
import io.acari.memory.UserSchema
import io.acari.memory.user.USER_INFORMATION_CHANNEL
import io.acari.memory.user.UserInfoRequest
import io.acari.memory.user.UserInfoResponse
import io.acari.util.loggerFor
import io.acari.util.toOptional
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.User
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.MaybeHelper
import io.vertx.reactivex.SingleHelper

object UserService {

  private val log = loggerFor(javaClass)

  fun findUserInformation(vertx: Vertx, user: User): Single<String> =
    Single.just(user)
      .filter { it is AccessToken }
      .map { it as AccessToken }
      .flatMap { accessToken ->
        extractOAuthUserInformation(accessToken)
      }
      .flatMapSingle { oauthUserInformation ->
        val userIdentifier = hashingFunction.hashString(oauthUserInformation.getString("email"), Charsets.UTF_16).toString()
        fetchUser(vertx, userIdentifier)
          .map {  rememberedUser -> Triple(userIdentifier, rememberedUser, oauthUserInformation)}
      }
      .map { extractUser(it) }

  private fun fetchUser(vertx: Vertx, userIdentifier: String): Single<JsonObject> =
    SingleHelper.toSingle<Message<UserInfoResponse>> { handler ->
      val eventBus = vertx.eventBus()
      eventBus.send(
        USER_INFORMATION_CHANNEL,
        UserInfoRequest(userIdentifier), handler
      )
    }.map { userResponse ->
      jsonObjectOf(
        UserSchema.GLOBAL_IDENTIFIER to userResponse.body().guid
      )
    }

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

  private fun extractUser(userInformations: Triple<String, JsonObject, JsonObject>): String {
    val idToken = userInformations.third
    return JsonObject()
      .put("fullName", idToken.getValue("name"))
      .put("userName", idToken.getValue("preferred_username"))
      .put("firstName", idToken.getValue("given_name"))
      .put("lastName", idToken.getValue("family_name"))
      .put("email", idToken.getValue("email"))
      .put("key", userInformations.first)
      .put(UserSchema.GLOBAL_IDENTIFIER, userInformations.second.getString(UserSchema.GLOBAL_IDENTIFIER))
      .encode()
  }
}
