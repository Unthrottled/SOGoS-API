package io.acari.http

import io.acari.developer.createStaticContentProxy
import io.acari.util.loggerFor
import io.acari.util.toOptional
import io.reactivex.Maybe
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.oauth2.AccessToken
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler

private val logger = loggerFor("APIRouter")

fun mountAPIRoute(vertx: Vertx, router: Router, configuration: JsonObject): Router {
  router.mountSubRouter("/api", createAPIRoute(vertx))

  // Static content path must be mounted last, as a fall back
  router.get("/*")
    .handler(fetchStaticContentHandler(vertx, configuration))
    .failureHandler { routingContext ->
      val statusCode = routingContext.statusCode()
      if(statusCode !=401 && statusCode != 403){
        routingContext.reroute("/")
      } else {
        routingContext.response().setStatusCode(404).end()
      }
    }

  return router
}

fun fetchStaticContentHandler(vertx: Vertx, configuration: JsonObject): Handler<RoutingContext> =
  createStaticContentProxy(vertx, configuration)
    .orElseGet {
      StaticHandler.create()
    }

fun createAPIRoute(vertx: Vertx): Router {
  val router = Router.router(vertx)
  router.get("/user").handler(createUserHandler())
  router.post("/action").handler(createActionsHandler())
  return router
}
