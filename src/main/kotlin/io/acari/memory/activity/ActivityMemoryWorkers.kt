package io.acari.memory.activity

import io.acari.memory.EffectListener
import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.util.loggerFor
import io.reactivex.Completable
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

const val CURRENT_ACTIVITY_CHANNEL = "activity.current"

object ActivityMemoryWorkers {

  val log = loggerFor(javaClass)

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    eventBus.consumer(CURRENT_ACTIVITY_CHANNEL, CurrentActivityListener(mongoClient))
    eventBus.consumer(EFFECT_CHANNEL, EffectListener(mongoClient, vertx))
    return Completable.complete()
  }
}
