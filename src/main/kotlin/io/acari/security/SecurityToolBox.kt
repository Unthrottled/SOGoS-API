package io.acari.security

import io.reactivex.Single
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.ext.auth.oauth2.OAuth2Auth
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions
import io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.reactivex.SingleHelper

fun createSecurityRouter(
  vertx: Vertx,
  oAuth2AuthProvider: OAuth2Auth,
  config: JsonObject
): Router {
  val router = Router.router(vertx)

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
