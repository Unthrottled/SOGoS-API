package io.acari.http

import io.acari.memory.Effect
import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.security.USER_IDENTIFIER
import io.acari.user.UserService
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import java.time.Instant

private val logger = loggerFor("UserRoutes")

fun createUserHandler(userService: UserService): Handler<RoutingContext> = Handler { routingContext ->
  userService.findUserInformation(routingContext.user())
    .subscribe({
      routingContext.response()
        .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        .end(it)
    }) {
      logger.error("Unable to get user for reasons!", it)
      routingContext.fail(404)
    }
}

const val USER_WELCOMED = "USER_WELCOMED"
const val TACMOD_NOTIFIED = "TACMOD_NOTIFIED"
const val TACMOD_DOWNLOADED = "TACMOD_DOWNLOADED"
const val TACMOD_THANKED = "TACMOD_THANKED"

fun createOnboardingRouter(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = Router.router(vertx)

  // What else can I say except, "You're Welcome"
  router.post("/welcomed").handler { requestContext ->
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        USER_WELCOMED,
        JsonObject(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().setStatusCode(201).end()
  }

  router.post("/TacMod/notified").handler { requestContext ->
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        TACMOD_NOTIFIED,
        JsonObject(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().setStatusCode(201).end()

  }

  router.post("/TacMod/downloaded").handler { requestContext ->
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        TACMOD_DOWNLOADED,
        JsonObject(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().setStatusCode(201).end()

  }

  router.post("/TacMod/thanked").handler { requestContext ->
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        TACMOD_THANKED,
        JsonObject(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().setStatusCode(201).end()
  }

  return router
}

fun createAuthorizedUserRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = Router.router(vertx)
  router.mountSubRouter("/onboarding", createOnboardingRouter(vertx, mongoClient))
  return router
}

