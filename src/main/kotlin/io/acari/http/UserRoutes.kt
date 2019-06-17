package io.acari.http

import io.acari.user.UserService
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.ext.web.RoutingContext
import io.vertx.reactivex.ext.mongo.MongoClient

private val logger = loggerFor("UserRoutes")

fun createUserHandler(vertx: Vertx, mongoClient: MongoClient): Handler<RoutingContext> = Handler { routingContext ->
  UserService.findUserInformation(vertx, routingContext.user())
    .subscribe({
      routingContext.response()
        .putHeader(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
        .end(it)
    }) {
      logger.warn("Unable to get user", it)
      routingContext.fail(404)
    }
}
