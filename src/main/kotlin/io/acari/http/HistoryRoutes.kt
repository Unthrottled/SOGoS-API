package io.acari.http

import io.acari.memory.ActivityHistorySchema
import io.acari.memory.activity.activityFromJson
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.vertx.core.json.Json
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router

private val logger = loggerFor("History Routes")

const val JSON_STREAM = "application/stream+json"

fun createHistoryRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = Router.router(vertx)
  router.get("/:userIdentifier/feed").handler { requestContext ->
    val userIdentifier = requestContext.request().getParam("userIdentifier")
    val response = requestContext.response()
    response.isChunked = true
    response.putHeader(HttpHeaderNames.CONTENT_TYPE, JSON_STREAM)
    mongoClient.findBatch(
      ActivityHistorySchema.COLLECTION, jsonObjectOf(
        ActivityHistorySchema.GLOBAL_USER_IDENTIFIER to userIdentifier
      )
    ).toFlowable()
      .map {
        Json.encode(activityFromJson(it))
      }
      .subscribe({
        response.write(it)
      }, {
        logger.warn("Unable to fetch activity feed for $userIdentifier because reasons.", it)
      }, {
        response.end()
      })
  }
  return router
}
