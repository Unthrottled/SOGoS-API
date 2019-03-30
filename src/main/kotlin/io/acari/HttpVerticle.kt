package io.acari

import io.vertx.core.Future
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions
import io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth
import io.vertx.reactivex.core.AbstractVerticle

class HttpVerticle: AbstractVerticle() {
  override fun start(startFuture: Future<Void>) {
    OpenIDConnectAuth.discover(
      vertx.delegate, OAuth2ClientOptions()
        .setSite("http://pringle:8080/auth/realms/master")
        .setClientID("sogos")
        .setAuthorizationPath("http://pringle:8080/auth/realms/master/protocol/openid-connect/auth")
    ) { result ->
      if (result.succeeded()) {
        result.map {oauth2 ->
          println(oauth2.flowType)
        }
      }
    }

    vertx
      .createHttpServer()
      .requestHandler { req ->
        req.response()
          .putHeader("content-type", "text/plain")
          .end("Hello from Vert.x!")
      }.rxListen(8888)
      .subscribe({
        startFuture.complete()
        println("HTTP server started on port 8888")
      }) {
        startFuture.fail("Unable to start HTTP Verticle because $")
      }
  }
}
