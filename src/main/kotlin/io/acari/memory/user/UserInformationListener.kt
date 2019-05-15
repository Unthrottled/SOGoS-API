package io.acari.memory.user

import io.acari.memory.UserSchema
import io.reactivex.Maybe
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient
import java.time.Instant
import java.util.*

class UserInformationListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<UserInfoRequest>> {
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
    val timeCreated = Instant.now().toEpochMilli()
    val usersGiud = UUID.randomUUID().toString()
    val userInformation = jsonObjectOf(
      UserSchema.GLOBAL_IDENTIFIER to usersGiud,
      UserSchema.OAUTH_IDENTIFIERS to jsonArrayOf(userInfoRequest.userIdentifier),
      UserSchema.TIME_CREATED to timeCreated
    )
    return mongoClient.rxInsert(UserSchema.COLLECTION, userInformation)
      .map { userInformation }
      .doAfterSuccess {
        vertx.eventBus().publish(NEW_USER_CHANNEL, UserCreatedEvent(usersGiud, timeCreated))
      }
  }

}
