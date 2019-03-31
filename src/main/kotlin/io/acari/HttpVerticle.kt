package io.acari

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions
import io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.OAuth2AuthHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.handler.UserSessionHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class HttpVerticle : AbstractVerticle() {
  override fun start(startFuture: Future<Void>) {
    OpenIDConnectAuth.discover(
      vertx, OAuth2ClientOptions()
        .setSite("http://pringle:8080/auth/realms/master")
        .setClientID("sogos")
    ) { result ->
      if (result.succeeded()) {
        result.map { oauth2 ->
          val router = Router.router(vertx)
          val authEngagmentRoute = router.route("/engage")
          router.route()
            .handler(CookieHandler.create())
            .handler(SessionHandler.create(LocalSessionStore.create(vertx)))
            .handler(UserSessionHandler.create(oauth2))
            .handler(UserSessionHandler.create(oauth2))
            .handler(OAuth2AuthHandler.create(oauth2)
              .setupCallback(authEngagmentRoute)
              .extraParams(
                json {
                  obj("redirect_uri" to "http://localhost:8888/engage")
                }
              )
              .addAuthorities(setOf("profile", "openid"))
            )
          router.get("/")
            .handler { req ->
              req.response()
                .putHeader("content-type", "text/plain")
                .end("Hello from Vert.x: ${req.user()}!")
            }

          vertx
            .createHttpServer()
            .requestHandler { request -> router.accept(request) }
            .listen(8888) {
              if(it.succeeded()){
                startFuture.complete()
                println("HTTP server started on port 8888")
              } else {
                startFuture.fail("Unable to start HTTP Verticle because ${it.cause().message}")
              }
            }

        }
      } else {
        startFuture.fail(result.cause())
      }
    }
  }
}
