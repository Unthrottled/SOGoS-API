package io.acari.http

import io.acari.util.loggerFor
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

private val logger = loggerFor("APIRouter")

fun mountAPIRoute(vertx: Vertx, router: Router, configuration: JsonObject): Router {
  router.mountSubRouter("/api", createAPIRoute(vertx))

  // Static content path must be mounted last, as a fall back
  router.get("/*")
    .failureHandler { routingContext ->
      val statusCode = routingContext.statusCode()
      if(statusCode != 401 && statusCode != 403){
        routingContext.reroute("/")
      } else {
        routingContext.response().setStatusCode(404).end()
      }
    }

  return router
}


fun createAPIRoute(vertx: Vertx): Router {
  val router = Router.router(vertx)
  router.get("/user").handler(createUserHandler(vertx))
  router.post("/action").handler(createActionHandler())
  router.post("/actions").handler(createActionsHandler())
  return router
}
