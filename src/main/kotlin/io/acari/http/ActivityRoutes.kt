package io.acari.http

import io.acari.memory.Effect
import io.acari.memory.activity.CURRENT_ACTIVITY_CHANNEL
import io.acari.memory.activity.CurrentActivityRequest
import io.acari.memory.activity.CurrentActivityResponse
import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.security.USER_IDENTIFIER
import io.acari.util.loggerFor
import io.acari.util.toOptional
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.Router.router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.SingleHelper
import java.time.Instant

private val logger = loggerFor("Activity Routes")

const val STARTED_ACTIVITY = "STARTED_ACTIVITY"
const val REMOVED_ACTIVITY = "REMOVED_ACTIVITY"
const val UPDATED_ACTIVITY = "UPDATED_ACTIVITY"

fun createActivityRoutes(vertx: Vertx): Router {
  val router = router(vertx)

  router.get("/current").handler { requestContext ->
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    SingleHelper.toSingle<Message<CurrentActivityResponse>> { handler ->
      vertx.eventBus()
        .send(CURRENT_ACTIVITY_CHANNEL, CurrentActivityRequest(userIdentifier), handler)
    }.map { it.body().activity }
      .subscribe({
        requestContext.response().setStatusCode(200).end(Json.encode(it))
      }) {
        logger.warn("Unable to service current activity request for $userIdentifier", it)
        requestContext.fail(500)
      }
  }

  /**
   * Should be used to assimilate any offline activities that may have
   * been performed.
   */
  router.post("/bulk").handler { requestContext ->
    val bodyAsJsonArray = requestContext.bodyAsJsonArray
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    bodyAsJsonArray.stream()
      .map { it as JsonObject }
      .forEach { activity ->
        //todo: some type of sanitization?
        vertx.eventBus().publish(
          EFFECT_CHANNEL,
          Effect(
            userIdentifier,
            Instant.now().toEpochMilli(),
            activity.getLong("antecedenceTime"),
            STARTED_ACTIVITY,
            activity.getJsonObject("content") ?: JsonObject(),
            extractValuableHeaders(requestContext)
          )
        )
      }
    requestContext.response().setStatusCode(200).end()
  }
  router.post().handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        bodyAsJson.getLong("antecedenceTime"),
        STARTED_ACTIVITY,
        bodyAsJson.getJsonObject("content") ?: JsonObject(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().setStatusCode(200).end()
  }

  router.put().handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        bodyAsJson.getLong("antecedenceTime"), // todo: does this matter in all contexts?
        UPDATED_ACTIVITY,
        bodyAsJson.getJsonObject("content") ?: JsonObject(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().setStatusCode(200).end()
  }

  router.delete().handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        bodyAsJson.getLong("antecedenceTime"), // todo: does this matter in all contexts?
        REMOVED_ACTIVITY,
        bodyAsJson.getJsonObject("content") ?: JsonObject(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().setStatusCode(200).end()
  }

  return router
}

fun extractValuableHeaders(requestContext: RoutingContext): JsonObject =
  requestContext.request().getHeader("User-Agent").toOptional()
    .map {
      jsonObjectOf(
        "userAgent" to it
      )
    }
    .orElseGet { jsonObjectOf() }
