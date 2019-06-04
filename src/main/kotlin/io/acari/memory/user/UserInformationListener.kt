package io.acari.memory.user

import io.acari.memory.Effect
import io.acari.memory.UserSchema
import io.acari.security.extractUserIdentificationKey
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
    val openIDInformation = userInfoRequest.openIDInformation
    val openIDUserIdentifier = extractUserIdentificationKey(openIDInformation)
    findUser(mongoClient, openIDUserIdentifier)
      .switchIfEmpty(createUser(openIDUserIdentifier, openIDInformation, mongoClient))
      .subscribe({ user ->
        message.reply(UserInfoResponse(user.getString(UserSchema.GLOBAL_USER_IDENTIFIER)))
      }) {
        UserMemoryWorkers.log.warn("Unable to fetch user for reasons.", it)
        message.fail(404, "Shit's broke yo")
      }
  }

  private fun findUser(
    mongoClient: MongoClient,
    openIDUserIdentifier: String
  ): Maybe<JsonObject> {
    return mongoClient.rxFindOne(
      UserSchema.COLLECTION, jsonObjectOf(
        UserSchema.OAUTH_IDENTIFIERS to jsonArrayOf(openIDUserIdentifier)
      ), jsonObjectOf()
    )
  }

  private fun createUser(
    openIDUserIdentifier: String,
    openIDInformation: JsonObject,
    mongoClient: MongoClient
  ): Maybe<JsonObject> {
    val timeCreated = Instant.now().toEpochMilli()
    val usersGiud = UUID.randomUUID().toString()
    val userInformation = jsonObjectOf(
      UserSchema.GLOBAL_USER_IDENTIFIER to usersGiud,
      UserSchema.OAUTH_IDENTIFIERS to jsonArrayOf(openIDUserIdentifier),
      UserSchema.TIME_CREATED to timeCreated
    )
    return mongoClient.rxInsert(UserSchema.COLLECTION, userInformation)
      .map { userInformation }
      .doAfterSuccess {
        vertx.eventBus().publish(EFFECT_CHANNEL, Effect(usersGiud, timeCreated, timeCreated, USER_CREATED, openIDInformation, jsonObjectOf()))
      }
  }
}

const val USER_CREATED = "USER_CREATED"
