package io.acari.http

import io.acari.memory.Effect
import io.acari.memory.HAS_SHARED_DASHBOARD
import io.acari.memory.SHARED_BRIDGE_CODE
import io.acari.memory.UserSchema
import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.security.*
import io.acari.types.NotFoundException
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.reactivex.Single
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.ext.jwt.jwtOptionsOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.auth.jwt.JWTAuth
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.handler.BodyHandler
import java.time.Instant

const val API_VERSION = "1.1.0"

private val logger = loggerFor("openRoutes")

const val ISSUED_READ_TOKEN = "ISSUED_READ_TOKEN"

fun attachNonSecuredRoutes(
  router: Router,
  configuration: JsonObject,
  mongoClient: MongoClient,
  jwtAuth: JWTAuth,
  vertx: Vertx
): Router {
  router.route()
    .handler(BodyHandler.create())

  router.get("/user/:shareCode/view/token").handler { routingContext ->
    val request = routingContext.request()
    val shareCode = request.getParam("shareCode")
    mongoClient.rxFindOne(
      UserSchema.COLLECTION, jsonObjectOf(
        "security.$SHARED_BRIDGE_CODE" to shareCode
      ), jsonObjectOf()
    ).filter { user ->
      user.getJsonObject(UserSchema.SECURITY_THINGS, jsonObjectOf())
        .getBoolean(HAS_SHARED_DASHBOARD, false)
    }.map {
      jsonObjectOf(
        "userIdentifier" to it.getString(UserSchema.GLOBAL_USER_IDENTIFIER),
        "readToken" to jwtAuth.generateToken(
          jsonObjectOf(UserSchema.GLOBAL_USER_IDENTIFIER to it.getString(UserSchema.GLOBAL_USER_IDENTIFIER))
          , jwtOptionsOf(
            expiresInMinutes = 5,
            issuer = SOGOS_ISSUER,
            algorithm = "RS256"
          )
        )
      )
    }
      .switchIfEmpty(Single.error(NotFoundException("Share code $shareCode does not exist")))
      .subscribe({
        val timeCreated = Instant.now().toEpochMilli()
        vertx.eventBus().publish(
          EFFECT_CHANNEL, Effect(
            it.getString("userIdentifier"),
            timeCreated,
            timeCreated,
            ISSUED_READ_TOKEN,
            jsonObjectOf(),
            extractValuableHeaders(routingContext)
          )
        )

        routingContext.response().setStatusCode(200)
          .putHeader(HttpHeaderNames.CACHE_CONTROL, "max-age=0, no-cache, must-revalidate, proxy-revalidate")
          .putHeader(HttpHeaderNames.EXPIRES, Instant.EPOCH.)
          .end(it.encode())
      }, {
        if (it !is NotFoundException) {
          logger.error("Unable to get read token for share code $shareCode for raisins", it)
        }
        routingContext.response().setStatusCode(403).end()
      })
  }

  router.get("/version").handler {
    it.response()
      .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
      .putHeader(HttpHeaderNames.CACHE_CONTROL, "max-age=0, no-cache, must-revalidate, proxy-revalidate")
      .end(
        jsonObjectOf(
          "version" to API_VERSION
        ).encode()
      )
  }

  router.get("/time").handler {
    it.response()
      .putHeader(HttpHeaderNames.CACHE_CONTROL, "max-age=0, no-cache, must-revalidate, proxy-revalidate")
      .putHeader(HttpHeaderNames.EXPIRES, Instant.EPOCH.)
      .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
      .end(
        jsonObjectOf(
          "currentTime" to Instant.now().toEpochMilli()
        ).encode()
      )
  }

  router.get("/configurations").handler {
    val securityConfigurations = configuration.getJsonObject("security")
    it.response()
      .putHeader(HttpHeaderNames.CACHE_CONTROL, "max-age=0, no-cache, must-revalidate, proxy-revalidate")
      .putHeader(HttpHeaderNames.EXPIRES, Instant.EPOCH.)
      .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
      .end(
        jsonObjectOf(
          "clientID" to getUIClientId(configuration, securityConfigurations),
          "appClientID" to getNativeClientId(configuration, securityConfigurations),
          "authorizationEndpoint" to getAuthEndpoint(configuration, securityConfigurations),
          "logoutEndpoint" to getLogoutEndpoint(configuration, securityConfigurations),
          "userInfoEndpoint" to getUserInfoEndpoint(configuration, securityConfigurations),
          "tokenEndpoint" to getTokenEndpoint(configuration, securityConfigurations),
          "openIDConnectURI" to getUIOpenIdProvider(configuration, securityConfigurations),
          "provider" to getProvider(configuration, securityConfigurations),
          "issuer" to getIssuer(configuration, securityConfigurations)
        ).encode()
      )
  }
  return router
}
