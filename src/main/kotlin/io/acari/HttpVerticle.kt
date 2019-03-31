package io.acari

import io.acari.http.mountAPIRoute
import io.acari.security.createSecurityRouter
import io.acari.security.setUpOAuth
import io.reactivex.Single
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.reactivex.SingleHelper

class HttpVerticle : AbstractVerticle() {

  override fun start(startFuture: Future<Void>) {
    setUpOAuth(vertx)
      .flatMap { oauth2 ->
        val securedRoute = createSecurityRouter(vertx, oauth2)
        val apiRouter = mountAPIRoute(securedRoute)
        startServer(apiRouter)
      }
      .subscribe({
        startFuture.complete()
        println("HTTP server started on port 8888")
      }) {
        startFuture.fail("Unable to start HTTP Verticle because ${it.message}")
      }
  }

  private fun startServer(router: Router): Single<HttpServer> =
    SingleHelper.toSingle { handler ->
      vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8888, handler)
    }
}
