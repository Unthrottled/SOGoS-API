package io.acari.memory.user

import io.acari.util.loggerFor
import io.reactivex.Completable
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

data class UserInfoRequest(val userIdentifier: String)
data class UserInfoResponse(override val guid: String): User

data class Effect(val guid: String,
                  val timeCreated: Long,
                  val name: String,
                  val content: JsonObject,
                  val meta: JsonObject)

interface User {
  val guid: String
}

const val USER_INFORMATION_CHANNEL = "user.information"
const val EFFECT_CHANNEL = "effects"

object UserMemoryWorkers {

  val log = loggerFor(javaClass)

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    eventBus.consumer(USER_INFORMATION_CHANNEL, UserInformationListener(mongoClient, vertx))
    return Completable.complete()
  }

}
