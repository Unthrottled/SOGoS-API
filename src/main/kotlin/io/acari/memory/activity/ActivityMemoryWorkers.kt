package io.acari.memory.activity

import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.util.loggerFor
import io.reactivex.Completable
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

object ActivityMemoryWorkers {

  val log = loggerFor(javaClass)

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    eventBus.consumer(EFFECT_CHANNEL, ActivityEffectListener(mongoClient, vertx))
    eventBus.consumer(CURRENT_ACTIVITY_CHANNEL, CompletedPomodoroListener(mongoClient, vertx))
    return Completable.complete()
  }
}


