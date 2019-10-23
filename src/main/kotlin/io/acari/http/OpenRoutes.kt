package io.acari.http

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
        "clientID" to securityConfigurations.getString("App-Client-Id"),
        "openIDConnectURI" to securityConfigurations.getString("OpenId-Connect-Provider"),
        "provider" to securityConfigurations.getString("provider")
      ).encode()
    )
  }
  return router
}
