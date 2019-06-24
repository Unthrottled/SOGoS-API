package io.acari.memory.user

import io.acari.memory.EffectListener
import io.acari.util.loggerFor
import io.reactivex.Completable
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

data class UserInfoRequest(val openIDInformation: JsonObject)
data class UserInfoResponse(override val guid: String): User

interface User {
  val guid: String
}

const val EFFECT_CHANNEL = "effects"

object UserMemoryWorkers {

  val log = loggerFor(javaClass)

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    eventBus.consumer(EFFECT_CHANNEL, EffectListener(mongoClient, vertx))
    return Completable.complete()
  }

}
