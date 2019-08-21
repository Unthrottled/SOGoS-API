package io.acari.http

import io.acari.memory.Effect
import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.security.USER_IDENTIFIER
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.Router.router
import java.time.Instant

const val UPDATED_POMODORO_SETTINGS = "UPDATED_POMODORO_SETTINGS"

fun createTacticalRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = router(vertx)
  router.post("/pomodoro/settings").handler {requestContext->
    val bodyAsJson = requestContext.bodyAsJson
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        UPDATED_POMODORO_SETTINGS,
        bodyAsJson,
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON).setStatusCode(200).end()
  }
  return router
}
