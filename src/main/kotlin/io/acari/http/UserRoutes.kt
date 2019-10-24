package io.acari.http

import io.acari.user.UserService
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Handler
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
