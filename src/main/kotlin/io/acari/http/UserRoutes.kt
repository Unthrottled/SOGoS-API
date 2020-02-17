package io.acari.http

import io.acari.user.UserService
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Handler
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext

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

fun createOnboardingRouter(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = Router.router(vertx)
  router.post("/welcomed").handler {
    routingContext ->

    // update user with new attributes
    TODO("Need to be implemented.")
  }

  router.post("/TacMod/notify").handler {
      routingContext ->

    // update user with new attributes
    TODO("Need to be implemented.")
  }

  router.post("/TacMod/downloaded").handler {
      routingContext ->

    // update user with new attributes
    TODO("Need to be implemented.")
  }

  router.post("/TacMod/thanked").handler {
      routingContext ->

    // update user with new attributes
    TODO("Need to be implemented.")
  }


  return router
}

fun createAuthorizedUserRoutes(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = Router.router(vertx)
  router.mountSubRouter("/onboarding", createOnboardingRouter(vertx, mongoClient))
  return router
}

