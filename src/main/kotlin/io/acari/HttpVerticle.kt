package io.acari

import CONNECTION_STRING
import io.acari.http.attachNonSecuredRoutes
import io.acari.http.mountAPIRoute
import io.acari.http.mountSupportingRoutes
import io.acari.memory.MemoryInitializations
import io.acari.security.*
import io.acari.util.loggerFor
import io.acari.util.toOptional
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.vertx.core.Future
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.JksOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.ext.auth.jwt.jwtAuthOptionsOf
import io.vertx.kotlin.ext.auth.keyStoreOptionsOf
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.core.http.HttpServer
import io.vertx.reactivex.ext.auth.jwt.JWTAuth.create
import io.vertx.reactivex.ext.auth.oauth2.OAuth2Auth
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router

class HttpVerticle : AbstractVerticle() {
  companion object {
    private val logger = loggerFor(HttpVerticle::class.java)

    init {
      System.setProperty("org.mongodb.async.type", "netty")
    }
  }

  override fun start(startFuture: Future<Void>) {
    val memoryConfiguration = config().getString(CONNECTION_STRING)
      .toOptional()
      .map { jsonObjectOf("connection_string" to it) }
      .orElseGet { config().getJsonObject("memory") }
    val mongoClient = MongoClient.createShared(vertx, memoryConfiguration)
    val configuration = config()
    setUpOAuth(vertx, configuration)
      .zipWith(setUpDB(mongoClient).toSingle { mongoClient },
        BiFunction<OAuth2Auth, MongoClient, Pair<OAuth2Auth, MongoClient>> { oauth2, mongoClientComplet ->
          Pair(
            oauth2,
            mongoClientComplet
          )
        })
      .flatMap { pair ->
        val (oauth2, reactiveMongoClient) = pair
        val router = Router.router(vertx)
        val corsRouter = attachCORSRouter(router, configuration)
        val jwtAuth = create(
          vertx, jwtAuthOptionsOf(
            keyStore = keyStoreOptionsOf(
              password = getKeystorePassword(configuration),
              path = getKeystore(configuration)
            )
          )
        )
        val configuredRouter = attachNonSecuredRoutes(corsRouter, configuration, reactiveMongoClient, jwtAuth)
        val securedRoute = attachSecurityToRouter(configuredRouter, oauth2, configuration)
        val supplementedRoutes = mountSupportingRoutes(vertx, securedRoute, configuration)
        val apiRouter = mountAPIRoute(vertx, reactiveMongoClient, supplementedRoutes)
        startServer(apiRouter)
      }
      .subscribe({
        startFuture.complete()
        val serverConfig = configuration.getJsonObject("server")
        logger.info(
          "HTTP${if (serverConfig.getBoolean("SSL-Enabled")) "S" else ""} server started on port ${getPortNumber(
            config(),
            serverConfig
          )}"
        )
      }) {
        startFuture.fail("Unable to start HTTP Verticle because ${it.message}")
      }
  }

  private fun setUpDB(mongoClient: MongoClient) =
    MemoryInitializations.setUpCollections(mongoClient)
      .andThen(MemoryInitializations.registerCodecs(vertx))
      .andThen(MemoryInitializations.registerMemoryWorkers(vertx, mongoClient))

  private fun startServer(router: Router): Single<HttpServer> {
    val serverConfig = config().getJsonObject("server")
    return vertx
      .createHttpServer(
        HttpServerOptions()
          .setSsl(serverConfig.getBoolean("SSL-Enabled"))
          .setKeyStoreOptions(
            JksOptions()
              .setPassword(serverConfig.getString("Keystore-Password"))
              .setPath(serverConfig.getString("Keystore-Path"))
          )
      )
      .requestHandler(router)
      .rxListen(getPortNumber(config(), serverConfig))
  }
}
