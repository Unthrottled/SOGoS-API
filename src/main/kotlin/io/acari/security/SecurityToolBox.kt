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

  // Session Management MUST be registered BEFORE callback route
  router.route()
    .handler(CookieHandler.create())
    .handler(BodyHandler.create())
    .handler(SessionHandler.create(LocalSessionStore.create(vertx))
      .setAuthProvider(oAuth2AuthProvider))

  // Callback Route MUST be BEFORE OAuth Handler
  val securityConfig = config.getJsonObject("security")
  val authEngagementRoute = router.route(securityConfig.getString("Callback-Path")) // I am CallBack Route

  router.route()
    .handler(
      OAuth2AuthHandler.create(
        oAuth2AuthProvider, // I am OAuth Handler
        securityConfig.getString("Callback-URL")
      )
        .setupCallback(authEngagementRoute)
        .addAuthorities(setOf("profile", "openid", "email"))
    )
    .handler(refreshTokenHandler)

  router.post("/logout").handler(logoutHandler)
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


val refreshTokenHandler: (RoutingContext) -> Unit = { context ->
  val accessToken = context.user() as AccessToken
  if (accessToken.expired()) {
    SingleHelper.toSingle<Void> {
      accessToken.refresh(it)
    }.subscribe({
      context.next()
    }) {
      context.fail(500)
    }
  } else {
    context.next()
  }
}

val logoutHandler: (RoutingContext) -> Unit = { context ->
  val user = context.user() as AccessToken
  SingleHelper.toSingle<Void> {
    user.logout(it)
  }.subscribe({
    context.session().destroy()
    context.response()
      .setStatusCode(202)
      .end("K Bai!")
  }) {
    context.response()
      .setStatusCode(404)
      .end()
  }
}
