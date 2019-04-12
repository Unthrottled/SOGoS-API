package io.acari.http

import io.acari.service.UserService
import io.acari.util.loggerFor
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

private val logger = loggerFor("UserRoutes")

fun createUserHandler(): Handler<RoutingContext> = Handler { request ->
  UserService.createUser(request.user())
    .subscribe({
      request.response()
        .putHeader("content-type", "application/json")
        .end(it)
    }, {
      logger.warn("Unable to get user", it)
      request.fail(404)
    }, createCompletionHandler(request))
}
