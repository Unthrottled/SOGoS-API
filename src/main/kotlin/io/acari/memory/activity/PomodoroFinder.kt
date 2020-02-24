package io.acari.memory.activity

import io.acari.memory.ActivityHistorySchema
import io.acari.memory.PomodoroCompletionHistorySchema
import io.acari.memory.PomodoroHistorySchema
import io.reactivex.Maybe
import io.reactivex.Single
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.ext.mongo.MongoClient
import java.time.Duration
import java.time.Instant

class PomodoroFinder(private val mongoClient: MongoClient) {

  fun findPomodoroCount(guid: String, from: Long, to: Long): Single<Long> {
    return mongoClient.rxCount(
      PomodoroHistorySchema.COLLECTION,
      jsonObjectOf(
        PomodoroHistorySchema.GLOBAL_USER_IDENTIFIER to guid,
        PomodoroHistorySchema.TIME_OF_ANTECEDENCE to buildToFromQuery(to, from)

      )
    )
  }

  @Deprecated("Timezones are hard", ReplaceWith("findPomodoroCount"))
  fun legacyFindPomodoroCount(guid: String): Triple<Long, JsonObject, Maybe<JsonObject>> {
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


fun buildToFromQuery(to: Long, from: Long): JsonObject {
  return jsonObjectOf(
    "\$lt" to to, // toot toot motherfucker
    "\$gte" to from
  )
}

