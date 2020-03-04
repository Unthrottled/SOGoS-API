package io.acari.http

import io.acari.memory.BUCKET_NAME
import io.acari.memory.Effect
import io.acari.memory.UserSchema
import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.security.USER_IDENTIFIER
import io.acari.types.NotFoundException
import io.acari.user.UserService
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.reactivex.Single
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Duration
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
const val AVATAR_UPLOAD_REQUESTED = "AVATAR_UPLOAD_REQUESTED"
const val AVATAR_UPLOADED = "AVATAR_UPLOADED"
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

fun createAuthorizedUserRoutes(
  vertx: Vertx,
  mongoClient: MongoClient,
  presigner: S3Presigner
): Router {
  val router = Router.router(vertx)
  router.mountSubRouter("/onboarding", createOnboardingRouter(vertx, mongoClient))
  router.mountSubRouter("/share", createSharingRouter(vertx, mongoClient))
  router.mountSubRouter("/profile", createProfileRouter(vertx, mongoClient, presigner))

  router.get("/:userIdentifier/profile").handler { requestContext ->
    val userIdentifier = requestContext.request().getParam("userIdentifier")

    mongoClient.rxFindOne(
        UserSchema.COLLECTION, jsonObjectOf(
          UserSchema.GLOBAL_USER_IDENTIFIER to userIdentifier
        ), jsonObjectOf()
      )
      .switchIfEmpty(Single.create { it.onError(NotFoundException("Could not Find User")) })
      .subscribe({ user ->
        requestContext.response()
          .setStatusCode(200)
          .putHeader("Content-Type", "application/json")
          .end(
            user.getJsonObject("profile", jsonObjectOf()).encode()
          )
      }) {
        if (it !is NotFoundException) {
          logger.error("Unable to retrieve $userIdentifier for raisins", it)
        }
        requestContext.response().setStatusCode(404).end()
      }
  }
  return router
}

fun createProfileRouter(
  vertx: Vertx,
  mongoClient: MongoClient,
  presigner: S3Presigner
): Router {
  val router = Router.router(vertx)

  router.post("/avatar/create").handler { requestContext ->
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)

    try {
      val presignedUrl = presigner.presignPutObject { presignRequest ->
        presignRequest.putObjectRequest { objectRequest ->
            objectRequest.bucket(BUCKET_NAME)
              .key(userIdentifier)
          }
          .signatureDuration(Duration.ofMinutes(2))
      }

      // todo: consume event and add to user
      vertx.eventBus().publish(
        EFFECT_CHANNEL, Effect(
          userIdentifier,
          timeCreated,
          timeCreated,
          AVATAR_UPLOAD_REQUESTED,
          jsonObjectOf(
            "key" to userIdentifier
          ),
          extractValuableHeaders(requestContext)
        )
      )
      requestContext.response().setStatusCode(200).end(
        presignedUrl.url().toString()
      )
    } catch (e: Exception) {
      requestContext.response().setStatusCode(500).end()
    }
  }

  router.post("/avatar/uploaded").handler { requestContext ->
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        AVATAR_UPLOADED,
        JsonObject(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().setStatusCode(204).end()
  }

  return router
}

const val ENABLED_SHARED_DASHBOARD = "ENABLED_SHARED_DASHBOARD"
const val DISABLED_SHARED_DASHBOARD = "DISABLED_SHARED_DASHBOARD"

fun createSharingRouter(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = Router.router(vertx)

  router.post("/dashboard/read").handler { requestContext ->
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    val requestBody = requestContext.bodyAsJson
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        ENABLED_SHARED_DASHBOARD,
        requestBody ?: jsonObjectOf(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().setStatusCode(201).end()
  }

  router.delete("/dashboard/read").handler { requestContext ->
    val timeCreated = Instant.now().toEpochMilli()
    val userIdentifier = requestContext.request().headers().get(USER_IDENTIFIER)
    vertx.eventBus().publish(
      EFFECT_CHANNEL, Effect(
        userIdentifier,
        timeCreated,
        timeCreated,
        DISABLED_SHARED_DASHBOARD,
        JsonObject(),
        extractValuableHeaders(requestContext)
      )
    )
    requestContext.response().setStatusCode(204).end()
  }

  return router
}

