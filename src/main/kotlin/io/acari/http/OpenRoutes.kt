package io.acari.http

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.json.jsonObjectOf

fun attachNonSecuredRoutes(router: Router, configuration: JsonObject): Router {
  router.get("/configurations").handler {
    val securityConfigurations = configuration.getJsonObject("security")
    it.response().end(
      jsonObjectOf(
        "callbackURI" to securityConfigurations.getString("callbackURI"),
        "clientID" to securityConfigurations.getString("Client-Id"),
        "openIDConnectURI" to securityConfigurations.getString("OpenId-Connect-Provider")
      ).encode() // todo: should consolidate configurations.
    )
  }
  return router
}
