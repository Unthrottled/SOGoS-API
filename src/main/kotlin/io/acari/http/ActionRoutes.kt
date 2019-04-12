package io.acari.http

import io.acari.util.loggerFor
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext

private val logger = loggerFor("ActionRoutes")

fun createActionsHandler(): Handler<RoutingContext> = Handler { requestContext ->
  val bodyAsJson = requestContext.bodyAsJson
  logger.info("Received dis: ${bodyAsJson.encodePrettily()}")
  requestContext.response().setStatusCode(200).end()
}
