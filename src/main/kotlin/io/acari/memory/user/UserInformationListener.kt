package io.acari.memory.user

import io.acari.memory.UserSchema
import io.reactivex.Maybe
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient
import java.util.*

class UserInformationListener(private val mongoClient: MongoClient) :
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
    val userInformation = jsonObjectOf(
      UserSchema.GLOBAL_IDENTIFIER to UUID.randomUUID().toString(),
      UserSchema.OAUTH_IDENTIFIERS to jsonArrayOf(userInfoRequest.userIdentifier)
    )
    return mongoClient.rxInsert(UserSchema.COLLECTION, userInformation)
      .map { userInformation }
  }

}
