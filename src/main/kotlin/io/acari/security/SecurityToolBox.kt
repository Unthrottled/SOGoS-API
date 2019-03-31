package io.acari.security

import io.reactivex.Single
import io.vertx.core.Vertx
import io.vertx.ext.auth.oauth2.OAuth2Auth
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions
import io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.OAuth2AuthHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.handler.UserSessionHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.reactivex.SingleHelper

fun createSecurityRouter(vertx: Vertx, oAuth2AuthProvider: OAuth2Auth): Router {
  val router = Router.router(vertx)
  val authEngagementRoute = router.route("/engage")
  router.route()
    .handler(CookieHandler.create())
    .handler(SessionHandler.create(LocalSessionStore.create(vertx)))
    .handler(UserSessionHandler.create(oAuth2AuthProvider))
    .handler(UserSessionHandler.create(oAuth2AuthProvider))
    .handler(
      OAuth2AuthHandler.create(oAuth2AuthProvider, "http://pringle:8888/engage")
        .setupCallback(authEngagementRoute)
        .addAuthorities(setOf("profile", "openid", "email"))
    )
  return router
}

fun setUpOAuth(vertx: Vertx): Single<OAuth2Auth> =
  SingleHelper.toSingle { handler ->
    OpenIDConnectAuth.discover(
      vertx, OAuth2ClientOptions()
        .setSite("http://pringle:8080/auth/realms/master")
        .setClientID("sogos")
        .setClientSecret(System.getenv("sogos.client.secret")), handler
    )
  }
