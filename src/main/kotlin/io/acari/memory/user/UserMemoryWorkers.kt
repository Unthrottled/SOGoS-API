package io.acari.memory.user

import io.acari.util.loggerFor
import io.reactivex.Completable
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

data class UserInfoRequest(val userIdentifier: String)
data class UserInfoResponse(override val guid: String): User
data class UserCreatedEvent(override val guid: String, val timeCreated: Long): User

interface User {
  val guid: String
}

const val USER_INFORMATION_CHANNEL = "user.information"
const val NEW_USER_CHANNEL = "new.user"

object UserMemoryWorkers {

  val log = loggerFor(javaClass)

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    eventBus.consumer(USER_INFORMATION_CHANNEL, UserInformationListener(mongoClient, vertx))
    return Completable.complete()
  }

}
