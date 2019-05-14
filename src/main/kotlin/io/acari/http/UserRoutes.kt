package io.acari.http

import io.acari.service.UserService
import io.acari.util.loggerFor
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext

private val logger = loggerFor("UserRoutes")

// todo: break user functionality into a verticle
fun createUserHandler(vertx: Vertx): Handler<RoutingContext> = Handler { routingContext ->
  UserService.findUserInformation(vertx, routingContext.user())
    .subscribe({
      routingContext.response()
        .putHeader("content-type", "application/json")
        .end(it)
    }) {
      logger.warn("Unable to get user", it)
      routingContext.fail(404)
    }
}
