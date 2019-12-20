package io.acari.http

import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.Router.*

fun createPomodoroRouter(vertx: Vertx, mongoClient: MongoClient): Router {
  val router = router(vertx) //todo just subscribe to the event log
  return router
}
