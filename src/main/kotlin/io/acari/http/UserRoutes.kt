package io.acari.http

import io.acari.user.UserService
import io.acari.util.loggerFor
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Handler
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.RoutingContext

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
