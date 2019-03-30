package io.acari

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.auth.oauth2.OAuth2ClientOptions
import io.vertx.ext.auth.oauth2.providers.OpenIDConnectAuth

class MainVerticle : AbstractVerticle() {

  override fun start(startFuture: Future<Void>) {
    OpenIDConnectAuth.discover(
      vertx, OAuth2ClientOptions()
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
      }
      .listen(8888) { http ->
        if (http.succeeded()) {
          startFuture.complete()
          println("HTTP server started on port 8888")
        } else {
          startFuture.fail(http.cause())
        }
      }

  }

}
