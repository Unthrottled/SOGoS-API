package io.acari.security

import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import io.acari.util.toMaybe
import io.acari.util.toOptional
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions
import io.vertx.ext.auth.oauth2.impl.OAuth2TokenImpl
import io.vertx.reactivex.SingleHelper
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.auth.oauth2.OAuth2Auth
import io.vertx.reactivex.ext.auth.oauth2.providers.OpenIDConnectAuth
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.handler.BodyHandler
import io.vertx.reactivex.ext.web.handler.OAuth2AuthHandler

fun attachSecurityToRouter(
  router: Router,
  oAuth2AuthProvider: OAuth2Auth,
  config: JsonObject
): Router {
  router.route()
    .handler(BodyHandler.create())

  router.route()
    .handler(OAuth2AuthHandler.create(oAuth2AuthProvider))

  return router
}

fun setUpOAuth(vertx: Vertx, config: JsonObject): Single<OAuth2Auth> =
  SingleHelper.toSingle { handler ->
    val securityConfig = config.getJsonObject("security")
    val openIdProvider =
      getOpenIdProvider(config, securityConfig)
    val clientId = getClient(config, securityConfig)
    OpenIDConnectAuth.discover(
      vertx, OAuth2ClientOptions()
        .setSite(openIdProvider)
        .setClientID(clientId)
        .setClientSecret(config.getString("sogos.client.secret")), handler
    )
  }

private val hashingFunction: HashFunction = Hashing.hmacSha256(
  System.getenv("sogos.hmac.key").toByteArray()
)

fun hashString(stringToHash: String): String =
  hashingFunction.hashString(stringToHash, Charsets.UTF_16).toString()

fun extractUserIdentificationKey(openIDInformation: JsonObject): Maybe<String> =
  openIDInformation.getString("email").toOptional()
    .map { it.toMaybe() }
    .orElseGet { Maybe.error(IllegalStateException("User does not have an email! $openIDInformation")) }
    .map { hashString(it) }

fun extractUserValidationKey(emailAddress: String, globalUserIdentifier: String): String =
  hashString("$emailAddress(◡‿◡✿)$globalUserIdentifier")

const val USER_IDENTIFIER = "User-Identifier"

fun createVerificationHandler(): Handler<RoutingContext> = Handler { routingContext ->
  val user = routingContext.user().delegate as OAuth2TokenImpl
  val headers = routingContext.request().headers()
  val verificationKey = headers.get("Verification") ?: ""
  val globalUserIdentifier = headers.get(USER_IDENTIFIER) ?: ""
  val email = user.accessToken().getString("email") ?: ""
  val generatedVerificationKey = extractUserValidationKey(email, globalUserIdentifier)
  if (verificationKey == generatedVerificationKey) {
    routingContext.next()
  } else {
    routingContext.response().setStatusCode(403).end()
  }
}


fun getClient(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString("sogos.client.id") ?: securityConfig.getString("Client-Id")

fun getOpenIdProvider(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString("sogos.openid.provider") ?: securityConfig.getString("OpenId-Connect-Provider")

fun getProvider(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString("sogos.provider") ?: securityConfig.getString("provider")

fun getUIClientId(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString("sogos.client.id.ui") ?: securityConfig.getString("App-Client-Id")
