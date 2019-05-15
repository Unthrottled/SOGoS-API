package io.acari.memory.user

import io.acari.util.loggerFor
import io.reactivex.Completable
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

data class UserInfoRequest(val userIdentifier: String)
data class UserInfoResponse(val guid: String)

const val USER_INFORMATION_CHANNEL = "user.information"

object UserMemoryWorkers {

  val log = loggerFor(javaClass)

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    eventBus.consumer(USER_INFORMATION_CHANNEL, UserInformationListener(mongoClient))
    return Completable.complete()
  }

}
