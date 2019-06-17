package io.acari.security

import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import io.reactivex.Single
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.OAuth2Auth
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions
import io.vertx.ext.auth.oauth2.impl.OAuth2TokenImpl
import io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.OAuth2AuthHandler
import io.vertx.reactivex.SingleHelper

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
    OpenIDConnectAuth.discover(
      vertx, OAuth2ClientOptions()
        .setSite(securityConfig.getString("OpenId-Connect-Provider"))
        .setClientID(securityConfig.getString("Client-Id"))
        .setClientSecret(config.getString("sogos.client.secret")), handler
    )
  }

private val hashingFunction: HashFunction = Hashing.hmacSha256(
  System.getenv("sogos.hmac.key").toByteArray()
)

fun hashString(stringToHash: String): String =
  hashingFunction.hashString(stringToHash, Charsets.UTF_16).toString()

fun extractUserIdentificationKey(openIDInformation: JsonObject): String =
  hashString(openIDInformation.getString("email"))

fun extractUserValidationKey(emailAddress: String, globalUserIdentifier: String): String =
  hashString("$emailAddress(◡‿◡✿)$globalUserIdentifier")

const val USER_IDENTIFIER = "User-Identifier"

fun createVerificationHandler(): Handler<RoutingContext> = Handler { routingContext ->
  val user = routingContext.user() as OAuth2TokenImpl
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
