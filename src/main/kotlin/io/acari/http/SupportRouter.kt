package io.acari.http

import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.micrometer.PrometheusScrapingHandler

fun mountSupportingRoutes(vertx: Vertx, router: Router, configuration: JsonObject): Router {
  router.route("/metrics").handler(PrometheusScrapingHandler.create())

  return router
}
