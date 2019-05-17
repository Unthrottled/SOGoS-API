package io.acari.http

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.micrometer.PrometheusScrapingHandler

fun mountSupportingRoutes(vertx: Vertx, router: Router, configuration: JsonObject): Router {
  router.route("/metrics").handler(PrometheusScrapingHandler.create())

  return router
}
