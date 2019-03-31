package io.acari

import io.acari.http.mountAPIRoute
import io.acari.security.createSecurityRouter
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions
import io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth

class HttpVerticle : AbstractVerticle() {
  override fun start(startFuture: Future<Void>) {
    OpenIDConnectAuth.discover(
      vertx, OAuth2ClientOptions()
        .setSite("http://pringle:8080/auth/realms/master")
        .setClientID("sogos")
        .setClientSecret(System.getenv("sogos.client.secret"))
    ) { result ->
      if (result.succeeded()) {
        result.map { oauth2 ->
          val securedRoute = createSecurityRouter(vertx, oauth2)
          val apiRouter = mountAPIRoute(securedRoute)
          vertx
            .createHttpServer()
            .requestHandler(apiRouter)
            .listen(8888) {
              if (it.succeeded()) {
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
