package io.acari.http

import io.acari.memory.Effect
import io.acari.memory.activity.CurrentActivityFinder
import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.security.USER_IDENTIFIER
import io.acari.util.loggerFor
import io.acari.util.toOptional
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.RoutingContext
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.Router.router
import java.time.Instant

private val logger = loggerFor("Activity Routes")

const val CREATED_OBJECTIVE = "CREATED_OBJECTIVE"
const val REMOVED_OBJECTIVE = "REMOVED_OBJECTIVE"
const val UPDATED_OBJECTIVE = "UPDATED_OBJECTIVE"


fun createObjectiveRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = router(vertx)
  // todo: other user's should be allowed to see other's objectives.
  router.get("/:objectiveId").handler { requestContext ->
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    val objectiveId = requestContext.request().getParam("objectiveId")
    println(objectiveId)
//    currentActivityListener.handle(userIdentifier)
//      .subscribe({
    requestContext.response()
      .putHeader(CONTENT_TYPE, APPLICATION_JSON)
      .setStatusCode(200)
      .end(Json.encode("butt"))
//      }) {
//        logger.warn("Unable to service current activity request for $userIdentifier", it)
//        requestContext.fail(500)
//      }
  }

  router.post("/:objectiveId/key-result").handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    val objectiveId = requestContext.request().getParam("objectiveId")
    println(objectiveId)
//    vertx.eventBus().publish(
//      EFFECT_CHANNEL, Effect(
//        userIdentifier,
//        timeCreated,
//        bodyAsJson.getLong("antecedenceTime"),
//        CREATED_OBJECTIVE,
//        bodyAsJson.getJsonObject("content") ?: JsonObject(),
//        extractValuableHeaders(requestContext)
//      )
//    )
    requestContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end()
  }

  router.post("/").handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        bodyAsJson.getLong("antecedenceTime"),
        CREATED_OBJECTIVE,
        bodyAsJson.getJsonObject("content") ?: JsonObject(),
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
        bodyAsJson.getLong("antecedenceTime"), // todo: does this matter in all contexts?
        UPDATED_OBJECTIVE,
        bodyAsJson.getJsonObject("content") ?: JsonObject(),
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
        bodyAsJson.getLong("antecedenceTime"), // todo: does this matter in all contexts?
        REMOVED_OBJECTIVE,
        bodyAsJson.getJsonObject("content") ?: JsonObject(),
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
