package io.acari.user

import io.acari.memory.AVATAR_UPLOADED_FIELD
import io.acari.memory.BUCKET_NAME
import io.acari.memory.UserSchema
import io.acari.memory.user.UserInformationFinder
import io.acari.security.extractUserValidationKey
import io.acari.util.loggerFor
import io.acari.util.toOptional
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.MaybeHelper
import io.vertx.reactivex.ext.auth.User
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Duration
import java.util.*

class UserService(
  private val userInformationFinder: UserInformationFinder,
  private val presigner: S3Presigner
) {

  private val log = loggerFor(javaClass)

  fun findUserInformation(user: User): Single<String> =
    findUser(user)
      .map { extractUser(it) }

  private fun findUser(
    user: User
  ): Single<Pair<JsonObject, JsonObject>> =
    extractUserInformation(user)
      .flatMapSingle { oauthUserInformation ->
        fetchUserFromMemories(oauthUserInformation)
          .map { rememberedUser ->
            Pair(rememberedUser, oauthUserInformation)
          }
      }

  private fun extractUserInformation(user: User): Maybe<JsonObject> =
    Single.just(user.delegate)
      .filter { it is AccessToken }
      .map { it as AccessToken }
      .flatMap { accessToken ->
        extractOAuthUserInformation(accessToken)
      }

  private fun fetchUserFromMemories(oauthUserInformation: JsonObject): Single<JsonObject> =
    userInformationFinder.handle(oauthUserInformation)
      .map { userResponse ->
        jsonObjectOf(
          UserSchema.GLOBAL_USER_IDENTIFIER to userResponse.getString(UserSchema.GLOBAL_USER_IDENTIFIER),
          UserSchema.MISC_USER_THINGS to userResponse.getJsonObject(UserSchema.MISC_USER_THINGS),
          UserSchema.SECURITY_THINGS to userResponse.getJsonObject(UserSchema.SECURITY_THINGS)
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
    val userJson = userInformation.first
    val globalUserIdentifier = userJson.getString(UserSchema.GLOBAL_USER_IDENTIFIER)
    val userVerificationKey = extractUserValidationKey(idToken.getString("email"), globalUserIdentifier)
    val userInfo = JsonObject()
      .put("fullName", idToken.getValue("name"))
      .put("userName", idToken.getValue("preferred_username"))
      .put("firstName", idToken.getValue("given_name"))
      .put("lastName", idToken.getValue("family_name"))
      .put("email", idToken.getValue("email"))
      .put(UserSchema.GLOBAL_USER_IDENTIFIER, globalUserIdentifier)
    val security = JsonObject()
      .put("verificationKey", userVerificationKey)
      .mergeIn(userJson.getJsonObject(UserSchema.SECURITY_THINGS, jsonObjectOf()))
    val misc = userJson.getJsonObject("misc") ?: jsonObjectOf()
    if (misc.getBoolean(AVATAR_UPLOADED_FIELD, false)) {
      getPresignedUrl(presigner, globalUserIdentifier)
        .ifPresent {
          presignedUrl -> userInfo.put("avatar", presignedUrl)
        }
    }
    return jsonObjectOf(
      "information" to userInfo,
      "security" to security,
      "misc" to misc
    ).encode()
  }

}
fun getPresignedUrl(presigner: S3Presigner, globalUserIdentifier: String?): Optional<String> {
  return try {
    presigner.presignGetObject { presignRequest ->
      presignRequest.getObjectRequest { request ->
        request
          .bucket(BUCKET_NAME)
          .key(globalUserIdentifier)
      }.signatureDuration(Duration.ofMinutes(60))
    }.url().toString().toOptional()
  } catch (e: Exception) {
    Optional.empty()
  }
}
