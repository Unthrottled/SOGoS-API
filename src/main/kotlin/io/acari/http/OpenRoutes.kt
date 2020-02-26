package io.acari.http

import io.acari.memory.HAS_SHARED_DASHBOARD
import io.acari.memory.UserSchema
import io.acari.security.*
import io.acari.types.NotFoundException
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.reactivex.Single
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.ext.jwt.jwtOptionsOf
import io.vertx.reactivex.ext.auth.jwt.JWTAuth
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import java.time.Instant

const val API_VERSION = "1.1.0"

private val logger = loggerFor("openRoutes")

fun attachNonSecuredRoutes(
  router: Router,
  configuration: JsonObject,
  mongoClient: MongoClient,
  jwtAuth: JWTAuth
): Router {
  router.get("/user/:userIdentifier/view/token").handler { routingContext ->
    val request = routingContext.request()
    val userIdentifier = request.getParam("userIdentifier")
    mongoClient.rxFindOne(
      UserSchema.COLLECTION, jsonObjectOf(
        UserSchema.GLOBAL_USER_IDENTIFIER to userIdentifier
      ), jsonObjectOf()
    ).filter { user ->
      user.getJsonObject(UserSchema.SECURITY_THINGS, jsonObjectOf())
        .getBoolean(HAS_SHARED_DASHBOARD, false)
    }.map {
      jsonObjectOf(
        "readToken" to jwtAuth.generateToken(it, jwtOptionsOf(
          expiresInMinutes = 5,
          issuer = SOGOS_ISSUER
        ))
      )
    }
      .switchIfEmpty(Single.error(NotFoundException("User $userIdentifier does not exist")))
      .subscribe({
        routingContext.response().setStatusCode(200)
          .end(it.encode())
      }, {
        if (it !is NotFoundException) {
          logger.error("Unable to get read token for user $userIdentifier for raisins", it)
        }
        routingContext.response().setStatusCode(403).end()
      })
  }

  router.get("/version").handler {
    it.response()
      .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
      .end(
        jsonObjectOf(
          "version" to API_VERSION
        ).encode()
      )
  }

  router.get("/time").handler {
    it.response()
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
