package io.acari.http

import io.acari.util.loggerFor
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.Router.*

private val logger = loggerFor("Time Routes")

fun createTimeRoute(vertx: Vertx): Router {
  val router = router(vertx)
  router.post("/test").handler { requestContext ->
    val bodyAsJson = requestContext.bodyAsJson
    logger.info("Received dis: ${bodyAsJson.encodePrettily()}")
    requestContext.response().setStatusCode(200).end("Heyyy")
  }
  return router
}
