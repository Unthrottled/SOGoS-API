package io.acari.memory.activity

import io.acari.memory.CurrentActivitySchema
import io.acari.util.loggerFor
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.ext.mongo.MongoClient

data class Activity(val antecedenceTime: Long, val content: JsonObject)

fun activityFromJson(activityJson: JsonObject): Activity {
  return Activity(
    antecedenceTime = activityJson.getLong(CurrentActivitySchema.TIME_OF_ANTECEDENCE),
    content = activityJson.getJsonObject(CurrentActivitySchema.CONTENT)
  )
}

class CurrentActivityFinder(private val mongoClient: MongoClient) {
  val log = loggerFor(javaClass)

  fun handle(userIdentifier: String): Single<Activity> {
    return findCurrentActivity(mongoClient, userIdentifier)
      .map {
        //TODO: REMOVE ONCE DATA IS LESS JANKY.
        if (it.containsKey(CurrentActivitySchema.CURRENT)) {
          it.getJsonObject(CurrentActivitySchema.CURRENT)
        } else {
          it
        }
      }
      .map { activityJson ->
        activityFromJson(activityJson)
      }.switchIfEmpty { observer: SingleObserver<in Activity> ->
        observer.onError(IllegalStateException("$userIdentifier has no current activity!"))
      }
  }
}

fun findCurrentActivity(
  mongoClient: MongoClient,
  globalUserIdentifier: String
): Maybe<JsonObject> {
  return mongoClient.rxFindOne(
    CurrentActivitySchema.COLLECTION, jsonObjectOf(
      CurrentActivitySchema.GLOBAL_USER_IDENTIFIER to globalUserIdentifier
    ), jsonObjectOf()
  )
}
