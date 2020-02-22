package io.acari.http

import io.acari.security.*
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.ext.web.Router
import java.time.Instant

const val API_VERSION = "1.1.0"

fun attachNonSecuredRoutes(router: Router, configuration: JsonObject): Router {
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
