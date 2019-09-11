package io.acari.memory.activity

import io.acari.memory.CurrentActivitySchema
import io.acari.util.loggerFor
import io.acari.util.toMaybe
import io.reactivex.Maybe
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.ext.mongo.MongoClient

class PreviousActivityFinder(private val mongoClient: MongoClient) {
  val log = loggerFor(javaClass)

  fun handle(userIdentifier: String): Maybe<Activity> {
    return findPreviousActivity(mongoClient, userIdentifier)
      .flatMap {
        if (it.containsKey(CurrentActivitySchema.PREVIOUS)) {
          it.getJsonObject(CurrentActivitySchema.PREVIOUS).toMaybe()
        } else {
          Maybe.empty()
        }
      }
      .map { activityJson ->
        activityFromJson(activityJson)
      }
  }

  fun findPreviousActivity(
    mongoClient: MongoClient,
    globalUserIdentifier: String
  ): Maybe<JsonObject> {
    return mongoClient.rxFindOne(
      CurrentActivitySchema.COLLECTION, jsonObjectOf(
        CurrentActivitySchema.GLOBAL_USER_IDENTIFIER to globalUserIdentifier
      ), jsonObjectOf()
    )
  }
}
