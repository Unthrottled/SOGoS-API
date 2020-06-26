package io.acari.memory.activity

import io.acari.memory.PomodoroHistorySchema
import io.reactivex.Single
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.ext.mongo.MongoClient

class PomodoroFinder(private val mongoClient: MongoClient) {

  fun findCompletedPomodoroCount(guid: String, from: Long, to: Long): Single<Long> {
    return mongoClient.rxCount(
      PomodoroHistorySchema.COLLECTION,
      jsonObjectOf(
        PomodoroHistorySchema.GLOBAL_USER_IDENTIFIER to guid,
        PomodoroHistorySchema.TIME_OF_ANTECEDENCE to buildToFromQuery(to, from)

      )
    )
  }
}

fun buildToFromQuery(to: Long, from: Long): JsonObject {
  return jsonObjectOf(
    "\$lt" to to, // toot toot motherfucker
    "\$gte" to from
  )
}
