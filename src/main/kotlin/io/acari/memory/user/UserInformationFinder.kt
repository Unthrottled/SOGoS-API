package io.acari.memory.user

import io.acari.memory.Effect
import io.acari.memory.UserSchema
import io.acari.security.extractUserIdentificationKey
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient
import java.time.Instant
import java.util.*

class UserInformationFinder(private val mongoClient: MongoClient, private val vertx: Vertx) {

  fun handle(openIDInformation: JsonObject): Single<String> {
    val openIDUserIdentifier = extractUserIdentificationKey(openIDInformation)
    return findUser(openIDUserIdentifier)
      .switchIfEmpty(createUser(openIDUserIdentifier, openIDInformation, mongoClient))
      .switchIfEmpty { singleObserver: SingleObserver<in JsonObject> ->
        singleObserver.onError(IllegalStateException("Unable to find user for $openIDUserIdentifier"))
      }.map { user ->
        user.getString(UserSchema.GLOBAL_USER_IDENTIFIER)
      }
  }

  private fun findUser(
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
        vertx.eventBus().publish(
          EFFECT_CHANNEL,
          Effect(usersGiud, timeCreated, timeCreated, USER_CREATED, openIDInformation, jsonObjectOf())
        )
      }
  }
}

const val USER_CREATED = "USER_CREATED"
