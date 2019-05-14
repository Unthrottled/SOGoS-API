package io.acari.memory.user

import io.acari.memory.UserSchema
import io.acari.util.POKOCodec
import io.acari.util.loggerFor
import io.reactivex.Completable
import io.reactivex.Maybe
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.EventBus
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient
import java.util.*
import java.util.function.Consumer

data class UserInfoRequest(val userIdentifier: String)
data class UserInfoResponse(val guid: String)

const val USER_INFORMATION_CHANNEL = "user.information"

class UserInformationListener(private val mongoClient: MongoClient): Handler<Message<UserInfoRequest>> {
  override fun handle(message: Message<UserInfoRequest>) {
    val userInfoRequest = message.body()
    findUser(mongoClient, userInfoRequest)
      .switchIfEmpty(createUser(userInfoRequest, mongoClient))
      .subscribe({ user ->
        message.reply(UserInfoResponse(user.getString(UserSchema.GLOBAL_IDENTIFIER)))
      }) {
        UserMemoryWorkers.log.warn("Unable to fetch user for reasons.", it)
        message.fail(404, "Shit's broke yo")
      }
  }


  private fun findUser(
    mongoClient: MongoClient,
    userInfoRequest: UserInfoRequest
  ): Maybe<JsonObject> {
    return mongoClient.rxFindOne(
      UserSchema.COLLECTION, jsonObjectOf(
        UserSchema.OAUTH_IDENTIFIERS to jsonArrayOf(userInfoRequest.userIdentifier)
      ), jsonObjectOf()
    )
  }

  private fun createUser(
    userInfoRequest: UserInfoRequest,
    mongoClient: MongoClient
  ): Maybe<JsonObject> {
    val userInformation = jsonObjectOf(
      UserSchema.GLOBAL_IDENTIFIER to UUID.randomUUID().toString(),
      UserSchema.OAUTH_IDENTIFIERS to jsonArrayOf(userInfoRequest.userIdentifier)
    )
    return mongoClient.rxInsert(UserSchema.COLLECTION, userInformation)
      .map { userInformation }
  }

}

class MemoryWorker<T>(
  val channelName: String,
  val handlerCreator: (MongoClient) -> Handler<Message<T>>
)

object UserMemoryWorkers {

  val log = loggerFor(javaClass)
  val workers: List<MemoryWorker<out Any>> = listOf(
    MemoryWorker(USER_INFORMATION_CHANNEL) { mongoClient -> UserInformationListener(mongoClient)}
  )

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    registerCodecs(eventBus)
    workers.forEach { worker ->
//      eventBus.consumer(worker.channelName, worker.handlerCreator(mongoClient))
    }
    return Completable.complete()
  }

  private fun registerCodecs(eventBus: EventBus) {
    eventBus.delegate.registerDefaultCodec(
      UserInfoRequest::class.java,
      POKOCodec(
        { json, testObject ->
          json.put(UserSchema.OAUTH_IDENTIFIERS, testObject.userIdentifier)
        },
        { jsonObject ->
          UserInfoRequest(jsonObject.getString(UserSchema.OAUTH_IDENTIFIERS))
        },
        UserInfoRequest::class.java.name
      )
    )
    eventBus.delegate.registerDefaultCodec(
      UserInfoResponse::class.java,
      POKOCodec(
        { json, testObject ->
          json.put(UserSchema.GLOBAL_IDENTIFIER, testObject.guid)
        },
        { jsonObject ->
          UserInfoResponse(jsonObject.getString(UserSchema.GLOBAL_IDENTIFIER))
        },
        UserInfoResponse::class.java.name
      )
    )

  }

}
