package io.acari.http

import io.acari.memory.ActivityHistorySchema
import io.acari.memory.activity.buildToFromQuery
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.vertx.core.json.Json
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.ext.mongo.findOptionsOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

private val logger = loggerFor("History Routes")

const val JSON_STREAM = "application/stream+json"
const val JSON = "application/json"

fun createHistoryRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = Router.router(vertx)
  router.get("/:userIdentifier/first/before").handler { requestContext ->
    val sortOrder = -1
    val comparisonString = "\$lt"
    handleFirstRequest(requestContext, mongoClient, sortOrder, comparisonString)
  }
  router.get("/:userIdentifier/first/after").handler { requestContext ->
    val sortOrder = 1
    val comparisonString = "\$gte"
    handleFirstRequest(requestContext, mongoClient, sortOrder, comparisonString)
  }

  router.get("/:userIdentifier/feed").handler { requestContext ->
    val request = requestContext.request()
    val userIdentifier = request.getParam("userIdentifier")
    val response = requestContext.response()
    val meow = Instant.now()
    val from = try {
      request.getParam("from").toLong()
    } catch (_: Throwable) {
      meow.minus(7, ChronoUnit.DAYS).toEpochMilli()
    }
    val to = try {
      request.getParam("to").toLong()
    } catch (_: Throwable) {
      meow.toEpochMilli()
    }

    val asArray = requestContext.request().params().get("asArray") == "true"

    response.isChunked = true
    response.putHeader(HttpHeaderNames.CONTENT_TYPE, if (asArray) JSON else JSON_STREAM)
    if (asArray) {
      response.write("[")
    }
    mongoClient.findBatch(
      ActivityHistorySchema.COLLECTION, jsonObjectOf(
        ActivityHistorySchema.GLOBAL_USER_IDENTIFIER to userIdentifier,
        ActivityHistorySchema.TIME_OF_ANTECEDENCE to buildToFromQuery(to, from)
      )
    ).toFlowable()
      .map {
        it.remove("_id")
        it.remove("guid")
        val json = Json.encode(it)
        if (asArray) "$json,"
        else json
      }
      .subscribe({
        response.write(it)
      }, {
        logger.warn("Unable to fetch activity feed for $userIdentifier because reasons.", it)
      }, {
        if (asArray) {
          response.write("]")
        }
        response.end()
      })
  }
  return router
}

private fun handleFirstRequest(
  requestContext: RoutingContext,
  mongoClient: MongoClient,
  sortOrder: Int,
  comparisonString: String
) {
  val request = requestContext.request()
  val userIdentifier = request.getParam("userIdentifier")
  val response = requestContext.response()
  try {
    val relativeTime = request.getParam("relativeTime").toLong(10)

    mongoClient.rxFindWithOptions(
      ActivityHistorySchema.COLLECTION, jsonObjectOf(
        ActivityHistorySchema.GLOBAL_USER_IDENTIFIER to userIdentifier,
        ActivityHistorySchema.TIME_OF_ANTECEDENCE to jsonObjectOf(
          comparisonString to relativeTime
        )
      ),
      findOptionsOf(sort = jsonObjectOf(ActivityHistorySchema.TIME_OF_ANTECEDENCE to sortOrder), limit = 1)
    )
      .map { it.firstOrNull() }
      .filter(Objects::nonNull)
      .map {
        it.remove("_id")
        it.remove(ActivityHistorySchema.GLOBAL_USER_IDENTIFIER)
        it
      }
      .subscribe({
        response.putHeader(HttpHeaderNames.CONTENT_TYPE, JSON)
        response
          .setStatusCode(200)
          .end(Json.encode(it))
      }, {
        response.setStatusCode(404).end()
      })
  } catch (e: Throwable) {
    response.setStatusCode(400).end("""Expected "relativeTime" with a number of long type in request """)
  }
}
