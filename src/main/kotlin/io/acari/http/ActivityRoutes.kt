package io.acari.http

import io.acari.memory.Effect
import io.acari.memory.UserSchema
import io.acari.memory.activity.Activity
import io.acari.memory.activity.CurrentActivityFinder
import io.acari.memory.activity.PomodoroFinder
import io.acari.memory.activity.PreviousActivityFinder
import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.security.USER_IDENTIFIER
import io.acari.types.NotFoundException
import io.acari.util.doOrElse
import io.acari.util.loggerFor
import io.acari.util.toOptional
import io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.netty.handler.codec.http.HttpResponseStatus
import io.reactivex.SingleObserver
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.http.HttpServerRequest
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.Router.router
import io.vertx.reactivex.ext.web.RoutingContext
import java.time.Duration
import java.time.Instant
import java.util.*

private val logger = loggerFor("Activity Routes")

const val STARTED_ACTIVITY = "STARTED_ACTIVITY"
const val REMOVED_ACTIVITY = "REMOVED_ACTIVITY"
const val UPDATED_ACTIVITY = "UPDATED_ACTIVITY"

const val CREATED = "CREATED"
const val UPDATED = "UPDATED"
const val DELETED = "DELETED"
val uploadStatus = setOf(CREATED, UPDATED, DELETED)
const val THREE_WEEKS_IN_MINUTES = 30240L

fun createActivityRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = router(vertx)
  val currentActivityListener = CurrentActivityFinder(mongoClient)
  router.get("/current").handler { requestContext ->
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    currentActivityListener.handle(userIdentifier)
      .subscribe({
        requestContext.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(200)
          .end(Json.encode(it))
      }) {
        logger.warn("Unable to service current activity request for $userIdentifier", it)
        requestContext.fail(500)
      }
  }
  val previousActivityListener = PreviousActivityFinder(mongoClient)
  router.get("/previous").handler { requestContext ->
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    previousActivityListener.handle(userIdentifier)
      .switchIfEmpty { observer: SingleObserver<in Activity> ->
        observer.onError(NotFoundException("$userIdentifier has no previous activity!"))
      }
      .subscribe({
        requestContext.response()
          .putHeader(CONTENT_TYPE, APPLICATION_JSON)
          .setStatusCode(200)
          .end(Json.encode(it))
      }) {
        when (it) {
          is NotFoundException -> requestContext.fail(HttpResponseStatus.NOT_FOUND.code())
          else -> {
            logger.warn("Unable to service current activity request for $userIdentifier", it)
            requestContext.fail(500)
          }
        }
      }
  }

  /**
   * Should be used to assimilate any offline activities that may have
   * been performed.
   */
  router.post("/bulk").handler { requestContext ->
    val bodyAsJsonArray = requestContext.bodyAsJsonArray
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    mongoClient.rxFindOne(
      UserSchema.COLLECTION,
      jsonObjectOf(
        UserSchema.GLOBAL_USER_IDENTIFIER to userIdentifier
      ), jsonObjectOf(
        UserSchema.TIME_CREATED to 1
      )
    )
      .map { Instant.ofEpochMilli(it.getLong(UserSchema.TIME_CREATED)) }
      .subscribe({ userTimeCreated ->
        bodyAsJsonArray.stream()
          .map { it as JsonObject }
          .filter { it.containsKey("activity") }
          .filter { cachedActivity ->
            val meow = Instant.now()
            val antecedenceTime = Instant.ofEpochMilli(
              cachedActivity.getJsonObject("activity")
                .getLong("antecedenceTime", 0L)
            )
            val duration = Duration.between(antecedenceTime, meow).toMinutes()

            duration in 0..THREE_WEEKS_IN_MINUTES &&
              userTimeCreated.isBefore(antecedenceTime) &&
              uploadStatus.contains(cachedActivity.getString("uploadType"))
          }
          .forEach { cachedActivity ->
            val activity = cachedActivity.getJsonObject("activity")
            vertx.eventBus().publish(
              EFFECT_CHANNEL,
              Effect(
                userIdentifier,
                Instant.now().toEpochMilli(),
                activity.getLong("antecedenceTime"),
                mapTypeToEffect(cachedActivity.getString("uploadType")),
                activity.getJsonObject("content") ?: JsonObject(),
                extractValuableHeaders(requestContext)
              )
            )
          }
        requestContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end()
      }, {
        logger.error("Unable to bulk upload for user $userIdentifier for raisins", it)
        requestContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(500).end()
      }) {
        requestContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end()
      }
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
        STARTED_ACTIVITY,
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
        bodyAsJson.getLong("antecedenceTime"),
        UPDATED_ACTIVITY,
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
        bodyAsJson.getLong("antecedenceTime"),
        REMOVED_ACTIVITY,
        bodyAsJson.getJsonObject("content") ?: JsonObject(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().putHeader(CONTENT_TYPE, APPLICATION_JSON).setStatusCode(200).end()
  }

  val pomodoroFinder = PomodoroFinder(mongoClient)

  router.get("/pomodoro/current/count").handler { requestContext ->
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    val request = requestContext.request()
    val possiblyFrom = extractTime(request, "from")
    val possiblyTo = extractTime(request, "to")
    possiblyFrom.flatMap { from ->
      possiblyTo.map { to -> from to to } // heh
    }.doOrElse(
      {
        val (from, to) = it
        pomodoroFinder.findCompletedPomodoroCount(userIdentifier, from, to)
          .map {
            jsonObjectOf(
              "count" to it
            )
          }
          .subscribe({
            requestContext.response()
              .putHeader(CONTENT_TYPE, APPLICATION_JSON)
              .setStatusCode(200)
              .end(Json.encode(it))
          }) {
            logger.warn("Unable to service pomodoro count request for $userIdentifier", it)
            requestContext.fail(500)
          }
      }
    ) {
      requestContext.response()
        .setStatusCode(400)
        .end("Required to and from query parameters to be a number")
    }
  }

  return router
}

private fun extractTime(
  request: HttpServerRequest,
  parameter: String
): Optional<Long> {
  return try {
    request.getParam(parameter).toLong().toOptional()
  } catch (_: Throwable) {
    Optional.empty()
  }
}

private val mappings = mapOf(
  CREATED to STARTED_ACTIVITY,
  UPDATED to UPDATED_ACTIVITY,
  DELETED to REMOVED_ACTIVITY
)

private fun mapTypeToEffect(uploadType: String): String =
  mappings[uploadType] ?: STARTED_ACTIVITY

fun extractValuableHeaders(requestContext: RoutingContext): JsonObject =
  requestContext.request().getHeader("User-Agent").toOptional()
    .map {
      jsonObjectOf(
        "userAgent" to it
      )
    }
    .orElseGet { jsonObjectOf() }
