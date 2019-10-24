package io.acari.http

import io.acari.security.getOpenIdProvider
import io.acari.security.getProvider
import io.acari.security.getUIClientId
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.ext.web.Router

fun attachNonSecuredRoutes(router: Router, configuration: JsonObject): Router {
  router.get("/api/configurations").handler {
    val securityConfigurations = configuration.getJsonObject("security")
    it.response()
      .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
      .end(
      jsonObjectOf(
        "clientID" to getUIClientId(configuration, securityConfigurations),
        "openIDConnectURI" to getOpenIdProvider(configuration, securityConfigurations),
        "provider" to getProvider(configuration, securityConfigurations)
      ).encode()
    )
  }
  return router
}
