package io.acari.memory.user

import io.acari.memory.*
import io.acari.util.loggerFor
import io.reactivex.Completable
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

const val EFFECT_CHANNEL = "effects"

object UserMemoryWorkers {

  val log = loggerFor(javaClass)

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    eventBus.consumer(EFFECT_CHANNEL, EffectListener(mongoClient, vertx))
    eventBus.consumer(EFFECT_CHANNEL, SOGoSUserEffectListener(mongoClient, vertx))
    eventBus.consumer(EFFECT_CHANNEL, TacModEffectListener(mongoClient, vertx))
    eventBus.consumer(EFFECT_CHANNEL, UserSharedEffectListener(mongoClient, vertx))
    eventBus.consumer(EFFECT_CHANNEL, UserAvatarEffectListener(mongoClient, vertx))
    return Completable.complete()
  }
}
