package io.acari

import io.acari.http.attachNonSecuredRoutes
import io.acari.http.mountAPIRoute
import io.acari.http.mountSupportingRoutes
import io.acari.security.attachSecurityToRouter
import io.acari.security.setUpOAuth
import io.acari.util.loggerFor
import io.reactivex.Single
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.JksOptions
import io.vertx.ext.web.Router
import io.vertx.reactivex.SingleHelper

class HttpVerticle : AbstractVerticle() {
  companion object {
      private val logger = loggerFor(HttpVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    val configuration = config()
    setUpOAuth(vertx, configuration)
      .flatMap { oauth2 ->
        val router = Router.router(vertx)
        val configuredRouter = attachNonSecuredRoutes(router, configuration)
        val securedRoute = attachSecurityToRouter(configuredRouter, oauth2, configuration)
        val supplementedRoutes = mountSupportingRoutes(vertx, securedRoute, configuration)
        val apiRouter = mountAPIRoute(vertx, supplementedRoutes, configuration)
        startServer(apiRouter)
      }
      .subscribe({
        startFuture.complete()
        val jsonObject = configuration.getJsonObject("server")
        logger.info("HTTP${if(jsonObject.getBoolean("SSL-Enabled"))"S" else ""} server started on port ${jsonObject.getInteger("port")}")
      }) {
        startFuture.fail("Unable to start HTTP Verticle because ${it.message}")
      }
  }

  private fun startServer(router: Router): Single<HttpServer> =
    SingleHelper.toSingle { handler ->
      val serverConfig = config().getJsonObject("server")
      vertx
        .createHttpServer(HttpServerOptions()
          .setSsl(serverConfig.getBoolean("SSL-Enabled"))
          .setKeyStoreOptions(JksOptions()
            .setPassword(serverConfig.getString("Keystore-Password"))
            .setPath(serverConfig.getString("Keystore-Path")))
        )
        .requestHandler(router)
        .listen(serverConfig.getInteger("port"), handler)
    }
}
