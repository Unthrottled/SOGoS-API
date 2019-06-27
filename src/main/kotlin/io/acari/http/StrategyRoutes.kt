package io.acari.http

import io.acari.memory.Effect
import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.security.USER_IDENTIFIER
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.Router.router
import java.time.Instant

private val logger = loggerFor("Activity Routes")

const val CREATED_OBJECTIVE = "CREATED_OBJECTIVE"
const val UPDATED_OBJECTIVE = "UPDATED_OBJECTIVE"
const val REMOVED_OBJECTIVE = "REMOVED_OBJECTIVE"

fun createObjectiveRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = router(vertx)

  router.post("/").handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        CREATED_OBJECTIVE,
        bodyAsJson,
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end()
  }

  router.put("/").handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        UPDATED_OBJECTIVE,
        bodyAsJson,
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end()
  }

  router.delete("/").handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        REMOVED_OBJECTIVE,
        bodyAsJson,
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end()
  }

  return router
}

fun createStrategyRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = router(vertx)
  router.mountSubRouter("/objectives", createObjectiveRoutes(vertx, mongoClient))
  return router
}
