package io.acari.memory.tactical

import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.util.loggerFor
import io.reactivex.Completable
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

object TacticalMemoryWorkers {

  val log = loggerFor(javaClass)

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    eventBus.consumer(EFFECT_CHANNEL, PomodoroEffectListener(mongoClient, vertx))
    return Completable.complete()
  }
}
