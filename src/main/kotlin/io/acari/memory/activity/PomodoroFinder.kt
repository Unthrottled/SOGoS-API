package io.acari.memory.activity

import io.acari.memory.PomodoroCompletionHistorySchema
import io.reactivex.Maybe
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.ext.mongo.MongoClient
import java.time.Duration
import java.time.Instant

class PomodoroFinder(private val mongoClient: MongoClient) {

  fun findPomodoroCount(guid: String): Triple<Long, JsonObject, Maybe<JsonObject>> {
    val currentDay = Duration.between(
        Instant.EPOCH,
        Instant.now()
    ).toDays()
    val query = jsonObjectOf(
        PomodoroCompletionHistorySchema.GLOBAL_USER_IDENTIFIER to guid,
        PomodoroCompletionHistorySchema.DAY to currentDay
    )
    val foundPomodoroCount = mongoClient.rxFindOne(
        PomodoroCompletionHistorySchema.COLLECTION, query,
        jsonObjectOf()
    )
    return Triple(currentDay, query, foundPomodoroCount)
  }
}
