package io.acari.memory.strategy

import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.util.loggerFor
import io.reactivex.Completable
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

object StrategyMemoryWorkers {

  val log = loggerFor(javaClass)

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    eventBus.consumer(EFFECT_CHANNEL, StrategyEffectListener(mongoClient, vertx))
    return Completable.complete()
  }
}
