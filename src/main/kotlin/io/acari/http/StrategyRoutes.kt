package io.acari.http

import io.acari.memory.*
import io.acari.memory.activity.activityFromJson
import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.security.USER_IDENTIFIER
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.reactivex.Flowable
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.Router.router
import java.time.Instant

private val logger = loggerFor("Activity Routes")

const val CREATED_OBJECTIVE = "CREATED_OBJECTIVE"
const val UPDATED_OBJECTIVE = "UPDATED_OBJECTIVE"
const val REMOVED_OBJECTIVE = "REMOVED_OBJECTIVE"
const val FOUND_OBJECTIVES = "foundObjectives"

fun createObjectiveRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = router(vertx)

  router.get("/").handler {requestContext ->
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    val response = requestContext.response()
    response.isChunked = true
    response.putHeader(CONTENT_TYPE, JSON_STREAM)
    mongoClient.aggregate(CurrentObjectiveSchema.COLLECTION, jsonArrayOf(
      jsonObjectOf("\$match" to
        jsonObjectOf(CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER to userIdentifier)),
      jsonObjectOf("\$lookup" to jsonObjectOf(
        "from" to ObjectiveHistorySchema.COLLECTION,
        "localField" to CurrentObjectiveSchema.OBJECTIVES,
        "foreignField" to ObjectiveHistorySchema.IDENTIFIER,
        "as" to FOUND_OBJECTIVES
      ))

    )).toFlowable()
      .flatMap {foundResult ->
        Flowable.fromIterable(foundResult.getJsonArray(FOUND_OBJECTIVES))
          .map { it as JsonObject }
      }
      .map {
        it.remove("_id")
        it
      }
      .map {
        Json.encodePrettily(it)
      }
      .subscribe({
        response.write(it)
      }, {
        logger.warn("Unable to fetch objectives for $userIdentifier because reasons.", it)
        response.setStatusCode(500).end()
      }, {
        response.end()
      })

  }

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
