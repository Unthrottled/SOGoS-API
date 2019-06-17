package io.acari.http

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
import io.vertx.reactivex.SingleHelper
import io.vertx.reactivex.ext.mongo.MongoClient

private val logger = loggerFor("History Routes")

fun createHistoryRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = Router.router(vertx)
  router.get("/feed").handler { requestContext ->
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    SingleHelper.toSingle<Message<CurrentActivityResponse>> { handler ->
      vertx.eventBus()
        .send(CURRENT_ACTIVITY_CHANNEL, CurrentActivityRequest(userIdentifier), handler)
    }.map { it.body().activity }
      .subscribe({
        requestContext.response()
          .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .setStatusCode(200)
          .end(Json.encode(it))
      }) {
        logger.warn("Unable to service current activity request for $userIdentifier", it)
        requestContext.fail(500)
      }
  }
  router.get("/feed/stream").handler { requestContext ->

    val response = requestContext.response()
    response.isChunked = true
    0.until(11).forEach {
      //language=JSON
      response.write(
        """{
  "ayy": "lmao",
  "count": $it
}"""
      )
    }
  }
  return router
}
