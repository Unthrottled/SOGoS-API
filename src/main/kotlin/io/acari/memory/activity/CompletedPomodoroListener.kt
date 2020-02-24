package io.acari.memory.activity

import io.acari.memory.ActivityHistorySchema
import io.acari.memory.CurrentActivitySchema
import io.acari.memory.PomodoroHistorySchema
import io.acari.memory.UserSchema
import io.acari.util.loggerFor
import io.acari.util.toSingle
import io.reactivex.Completable
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.UpdateOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient

class CompletedPomodoroListener(
  private val mongoClient: MongoClient,
  private val vertx: Vertx
) : Handler<Message<JsonObject>> {
  val log = loggerFor(javaClass)
  override fun handle(event: Message<JsonObject>) {
    event.body().toSingle()
      .map { it.getJsonObject(CurrentActivitySchema.CURRENT) }
      .filter { it.getJsonObject(ActivityHistorySchema.CONTENT).getString("name") == "RECOVERY" }
      .filter { it.getJsonObject(ActivityHistorySchema.CONTENT).getBoolean("autoStart", false) }
      .flatMapCompletable { writePomodoroCompletion(event.body()) }
      .subscribe({
      }) {
        log.error("Unable to write completed pomodoro for event ${event.body()}", it)
      }
  }

  private fun writePomodoroCompletion(completionEffect: JsonObject): Completable {
    val guid = completionEffect.getString(UserSchema.GLOBAL_USER_IDENTIFIER)
    val completionDocument = jsonObjectOf(
      PomodoroHistorySchema.GLOBAL_USER_IDENTIFIER to guid,
      PomodoroHistorySchema.ACTIVITY_ID to completionEffect.getJsonObject(CurrentActivitySchema.PREVIOUS)
        .getJsonObject(ActivityHistorySchema.CONTENT).getString("uuid"),
      PomodoroHistorySchema.TIME_OF_ANTECEDENCE to completionEffect.getJsonObject(CurrentActivitySchema.CURRENT)
        .mapTo(Activity::class.java).antecedenceTime
    )
    return mongoClient
      .rxInsert(PomodoroHistorySchema.COLLECTION, completionDocument)
      .ignoreElement()
  }
}

