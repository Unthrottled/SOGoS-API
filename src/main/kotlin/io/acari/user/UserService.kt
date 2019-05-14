package io.acari.user

import io.acari.memory.UserSchema
import io.acari.memory.user.USER_INFORMATION_CHANNEL
import io.acari.memory.user.UserInfoRequest
import io.acari.memory.user.UserInfoResponse
import io.acari.security.hashString
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
    findUser(user, vertx)
      .map { extractUser(it) }

  private fun findUser(
    user: User,
    vertx: Vertx
  ): Single<Pair<JsonObject, JsonObject>> =
    extractUserInformation(user)
      .flatMapSingle { oauthUserInformation ->
        val oauthUserIdentifier = hashString(oauthUserInformation.getString("email"))
        fetchUserFromMemories(vertx, oauthUserIdentifier)
          .map { rememberedUser ->
            Pair(rememberedUser, oauthUserInformation) }
      }

  private fun extractUserInformation(user: User): Maybe<JsonObject> =
    Single.just(user)
      .filter { it is AccessToken }
      .map { it as AccessToken }
      .flatMap { accessToken ->
        extractOAuthUserInformation(accessToken)
      }

  private fun fetchUserFromMemories(vertx: Vertx, oauthUserIdentifier: String): Single<JsonObject> =
    SingleHelper.toSingle<Message<UserInfoResponse>> { handler ->
      val eventBus = vertx.eventBus()
      eventBus.send(
        USER_INFORMATION_CHANNEL,
        UserInfoRequest(oauthUserIdentifier), handler
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
        log.warn("Unable to fetch user info from OpenID provider, falling back to access token", it)
        accessToken
      }
  }

  private fun extractUser(userInformation: Pair<JsonObject, JsonObject>): String {
    val idToken = userInformation.second
    return JsonObject()
      .put("fullName", idToken.getValue("name"))
      .put("userName", idToken.getValue("preferred_username"))
      .put("firstName", idToken.getValue("given_name"))
      .put("lastName", idToken.getValue("family_name"))
      .put("email", idToken.getValue("email"))
      .put(UserSchema.GLOBAL_IDENTIFIER, userInformation.first.getString(UserSchema.GLOBAL_IDENTIFIER))
      .encode()
  }
}
