package io.acari.http

import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.memory.user.Effect
import io.acari.util.loggerFor
import io.acari.util.toOptional
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.Router.router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.jsonObjectOf
import java.time.Instant

private val logger = loggerFor("Activity Routes")

const val STARTED_ACTIVITY = "STARTED_ACTIVITY"
const val REMOVED_ACTIVITY = "REMOVED_ACTIVITY"
const val UPDATED_ACTIVITY = "UPDATED_ACTIVITY"
const val COMPLETED_ACTIVITY = "COMPLETED_ACTIVITY"

fun createActivityRoutes(vertx: Vertx): Router {
  val router = router(vertx)

  // this should accept the model that I have not created yet ._.
  router.post("/bulk").handler { requestContext ->
    val bodyAsJsonArray = requestContext.bodyAsJsonArray
    bodyAsJsonArray.stream()
      .map { it as JsonObject }
      .forEach { activity ->
        //todo: some type of sanitization?
        vertx.eventBus().publish(EFFECT_CHANNEL,
          Effect(
            activity.getString("guid"),
            Instant.now().toEpochMilli(),
            activity.getLong("antecedenceTime"),
            STARTED_ACTIVITY,
            activity.getJsonObject("activity"),
            extractValuableHeaders(requestContext)
          )
        )
      }
    requestContext.response().setStatusCode(200).end()
  }
  router.post("/start").handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    vertx.eventBus().publish(EFFECT_CHANNEL, Effect(
      bodyAsJson.getString("guid"),
      timeCreated,
      timeCreated,
      STARTED_ACTIVITY,
      bodyAsJson.getJsonObject("activity"),
      extractValuableHeaders(requestContext)
    ))
    requestContext.response().setStatusCode(200).end()
  }

  router.post("/stop").handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    vertx.eventBus().publish(EFFECT_CHANNEL, Effect(
      bodyAsJson.getString("guid"),
      timeCreated,
      timeCreated,
      COMPLETED_ACTIVITY,
      bodyAsJson.getJsonObject("activity"),
      extractValuableHeaders(requestContext)
    ))
    requestContext.response().setStatusCode(200).end()
  }

  router.put().handler {requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    vertx.eventBus().publish(EFFECT_CHANNEL, Effect(
      bodyAsJson.getString("guid"),
      timeCreated,
      timeCreated, // todo: does this matter in all contexts?
      UPDATED_ACTIVITY,
      bodyAsJson.getJsonObject("activity"),
      extractValuableHeaders(requestContext)
    ))
    requestContext.response().setStatusCode(200).end()
  }

  //prolly needs to have authorization (do not want others able to delete other's activities)
  router.delete().handler {requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    vertx.eventBus().publish(EFFECT_CHANNEL, Effect(
      bodyAsJson.getString("guid"),
      timeCreated,
      timeCreated, // todo: does this matter in all contexts?
      REMOVED_ACTIVITY,
      bodyAsJson.getJsonObject("activity"),
      extractValuableHeaders(requestContext)
    ))
    requestContext.response().setStatusCode(200).end()
  }

  return router
}

fun extractValuableHeaders(requestContext: RoutingContext): JsonObject =
  requestContext.request().getHeader("User-Agent").toOptional()
    .map { jsonObjectOf(
      "userAgent" to it
    ) }
    .orElseGet { jsonObjectOf() }
