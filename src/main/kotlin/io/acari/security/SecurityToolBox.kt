package io.acari.security

import AUTH_URL
import CLIENT_ID
import CLIENT_ID_UI
import CORS_ORIGIN_URL
import HMAC_KEY
import LOGOUT_URL
import OPENID_PROVIDER
import OPENID_PROVIDER_UI
import PORT_NUMBER
import PROVIDER
import TOKEN_URL
import USER_INFO_URL
import com.google.common.hash.HashFunction
import com.google.common.hash.Hashing
import io.acari.util.toMaybe
import io.acari.util.toOptional
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod.*
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
import io.vertx.reactivex.ext.web.handler.CorsHandler
import io.vertx.reactivex.ext.web.handler.OAuth2AuthHandler

fun attachCORSRouter(
  router: Router,
  config: JsonObject
): Router {
  router.route().handler(createCORSHandler(config))
  return router
}

fun createCORSHandler(config: JsonObject): Handler<RoutingContext>? {
  val securityConfig = config.getJsonObject("security")
  return CorsHandler.create(getCORSOrigin(config, securityConfig))
    .allowCredentials(true)
    .allowedHeaders(
      setOf(
        "x-requested-with",
        "Access-Control-Allow-Origin",
        "origin",
        "Content-Type",
        "accept",
        "X-Amz-Date",
        "X-Api-Key",
        "X-Amz-Security-Token",
        "Sec-Fetch-Mode",

        //SOGoS Things
        "Authorization",
        "Verification",
        "User-Identifier",
        "User-Agent"
      )
    )
    .allowedMethods(
      setOf(
        DELETE, GET, HEAD, OPTIONS, PATCH, POST, PUT
      )
    )
}


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
    val clientId = getClient(config, securityConfig)//todo: consolidate client ids since not confidential anymoar
    OpenIDConnectAuth.discover(
      vertx, OAuth2ClientOptions()
        .setSite(openIdProvider)
        .setClientID(clientId), handler
    )
  }

private val hashingFunction: HashFunction = Hashing.hmacSha256(
  System.getenv(HMAC_KEY).toByteArray()
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
): String = config.getString(CLIENT_ID) ?: securityConfig.getString("Client-Id")

fun getOpenIdProvider(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString(OPENID_PROVIDER) ?: securityConfig.getString("OpenId-Connect-Provider")

fun getAuthEndpoint(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString(AUTH_URL) ?: securityConfig.getString("auth-url")

fun getLogoutEndpoint(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString(LOGOUT_URL) ?: securityConfig.getString("logout-url")

fun getTokenEndpoint(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString(TOKEN_URL) ?: securityConfig.getString("token-url")

fun getUserInfoEndpoint(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString(USER_INFO_URL) ?: securityConfig.getString("user-info-url")

fun getUIOpenIdProvider(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString(OPENID_PROVIDER_UI)
  ?: config.getString(OPENID_PROVIDER)
  ?: securityConfig.getString("OpenId-Connect-Provider")

fun getProvider(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString(PROVIDER) ?: securityConfig.getString("provider")

fun getUIClientId(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString(CLIENT_ID_UI) ?: securityConfig.getString("App-Client-Id")

fun getCORSOrigin(
  config: JsonObject,
  securityConfig: JsonObject
): String = config.getString(CORS_ORIGIN_URL) ?: securityConfig.getString("allowed-origin")

fun getPortNumber(
  config: JsonObject,
  serverConfig: JsonObject
): Int = config.getInteger(PORT_NUMBER) ?: serverConfig.getInteger("port")
