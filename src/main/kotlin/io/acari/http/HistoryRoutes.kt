package io.acari.http

import io.acari.memory.ActivityHistorySchema
import io.acari.memory.activity.CURRENT_ACTIVITY_CHANNEL
import io.acari.memory.activity.CurrentActivityRequest
import io.acari.memory.activity.CurrentActivityResponse
import io.acari.security.USER_IDENTIFIER
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.SingleHelper
import io.vertx.reactivex.ext.mongo.MongoClient

private val logger = loggerFor("History Routes")

fun createHistoryRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = Router.router(vertx)
  router.get("/feed").handler { requestContext ->
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    val response = requestContext.response()
    response.isChunked = true
    mongoClient.findBatch(ActivityHistorySchema.COLLECTION, jsonObjectOf(
      ActivityHistorySchema.GLOBAL_USER_IDENTIFIER to userIdentifier
    )).toFlowable()
      .subscribe({
        response.write(it.encodePrettily())
      }, {
        logger.warn("Unable to fetch activity feed for $userIdentifier because reasons.", it)
      }, {
        response.close()
      })
  }
  return router
}
