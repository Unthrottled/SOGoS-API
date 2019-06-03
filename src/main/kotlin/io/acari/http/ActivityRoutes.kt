package io.acari.http

import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.memory.user.Effect
import io.acari.security.USER_IDENTIFIER
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

fun createActivityRoutes(vertx: Vertx): Router {
  val router = router(vertx)

  router.get("/current").handler { requestContext ->
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    requestContext.response()
      .setStatusCode(200)
      .end(
        jsonObjectOf(
          "antecedenceTime" to 1559556836772L,
          "content" to jsonObjectOf(
            "name" to "SOME_ACTIVITY",
            "uuid" to "08d88b69-8330-4d4e-b52f-d2075da7398e"
          )
        ).encode()
      )
  }

  // this should accept the model that I have not created yet ._.

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
