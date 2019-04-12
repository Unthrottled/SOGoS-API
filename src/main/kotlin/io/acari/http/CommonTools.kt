package io.acari.http

import io.acari.util.loggerFor
import io.vertx.ext.web.RoutingContext

private val logger = loggerFor("CommonTools")

fun createCompletionHandler(request: RoutingContext): () -> Unit {
  return {
    val response = request.response()
    if (!response.ended()) {
      logger.warn("${request.currentRoute()}'s observable completed before response was ended!")
      request.fail(404)
    }
  }
}
