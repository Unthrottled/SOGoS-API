package io.acari.memory.user

import io.acari.util.POKOCodec
import io.acari.util.loggerFor
import io.reactivex.Completable
import io.reactivex.Maybe
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.EventBus
import io.vertx.reactivex.ext.mongo.MongoClient
import java.util.*

data class UserInfoRequest(val userIdentifier: String)
data class UserInfoResponse(val guid: String)

const val USER_INFORMATION_CHANNEL = "user.information"

object UserMemoryWorkers {

  val log = loggerFor(javaClass)

  fun registerWorkers(vertx: Vertx, mongoClient: MongoClient): Completable {
    val eventBus = vertx.eventBus()
    registerCodecs(eventBus)
      eventBus.consumer<UserInfoRequest>(USER_INFORMATION_CHANNEL){
          message ->
        val userInfoRequest = message.body()
        mongoClient.rxFindOne("user", jsonObjectOf(
          "userIdentifiers" to jsonArrayOf(userInfoRequest.userIdentifier)
        ), jsonObjectOf( ))
          .switchIfEmpty (createUser(userInfoRequest, mongoClient))
          .subscribe({
            user->
            message.reply(UserInfoResponse(user.getString("guid")))
          }) {
            log.warn("Unable to fetch user for reasons.", it)
            message.fail(404, "Shit's broke yo")
          }

      }
    return Completable.complete()
  }

  private fun createUser(
    userInfoRequest: UserInfoRequest,
    mongoClient: MongoClient
  ): Maybe<JsonObject> {
    val userInformation = jsonObjectOf(
      "guid" to UUID.randomUUID().toString(),
      "userIdentifiers" to jsonArrayOf(userInfoRequest.userIdentifier)
    )
    return mongoClient.rxInsert("user", userInformation)
      .map { userInformation }
  }

  private fun registerCodecs(eventBus: EventBus) {
    eventBus.delegate.registerDefaultCodec(UserInfoRequest::class.java,
      POKOCodec(
        { json, testObject ->
          json.put("userIdentifier", testObject.userIdentifier)
        },
        { jsonObject ->
          UserInfoRequest(jsonObject.getString("userIdentifier"))
        },
        UserInfoRequest::class.java.name
      )
    )
    eventBus.delegate.registerDefaultCodec(UserInfoResponse::class.java,
      POKOCodec(
        { json, testObject ->
          json.put("guid", testObject.guid)
        },
        { jsonObject ->
          UserInfoResponse(jsonObject.getString("guid"))
        },
        UserInfoResponse::class.java.name
      )
    )

  }

}
