package io.acari.memory.activity

import io.acari.memory.Effect
import io.acari.memory.PomodoroCompletionHistorySchema.COLLECTION
import io.acari.memory.PomodoroCompletionHistorySchema.COUNT
import io.acari.memory.PomodoroCompletionHistorySchema.DAY
import io.acari.memory.PomodoroCompletionHistorySchema.GLOBAL_USER_IDENTIFIER
import io.acari.util.loggerFor
import io.acari.util.toSingle
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.MaybeObserver
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.UpdateOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient
import java.time.Duration
import java.time.Instant

class CompletedPomodoroListener(
  private val mongoClient: MongoClient,
  private val vertx: Vertx
) : Handler<Message<Effect>> {
  val log = loggerFor(javaClass)
  private val pomodoroFinder = PomodoroFinder(mongoClient)

  override fun handle(event: Message<Effect>) {
    event.body().toSingle()
      .filter { it.name == "STARTED_ACTIVITY" }
      .filter { it.content.getString("name") == "RECOVERY" }
      .filter { it.content.getBoolean("autoStart") }
      .flatMapCompletable { writePomodoroCount(it) }
      .subscribe({

      }) {
        log.error("Unable to process completed pomodoro for event ${event.body()}", it)
      }
  }

  private fun writePomodoroCount(completionEffect: Effect): Completable {
    val (currentDay, query, foundPomodoroCount) =
      pomodoroFinder.findPomodoroCount(completionEffect.guid)
    return foundPomodoroCount
      .map { item ->
        item.put(COUNT, item.getInteger(COUNT) + 1)
        item
      }.switchIfEmpty { observer: MaybeObserver<in JsonObject> ->
        observer.onSuccess(
          jsonObjectOf(
            GLOBAL_USER_IDENTIFIER to completionEffect.guid,
            DAY to currentDay,
            COUNT to 1
          )
        )
      }
      .flatMapCompletable {
        mongoClient
          .rxReplaceDocumentsWithOptions(COLLECTION, query, it, UpdateOptions(true))
          .ignoreElement()
      }
  }

}

class PomodoroFinder(private val mongoClient: MongoClient) {

  fun findPomodoroCount(guid: String): Triple<Long, JsonObject, Maybe<JsonObject>> {
    val currentDay = Duration.between(Instant.EPOCH, Instant.now()).toDays()
    val query = jsonObjectOf(
      GLOBAL_USER_IDENTIFIER to guid,
      DAY to currentDay
    )
    val foundPomodoroCount = mongoClient.rxFindOne(
      COLLECTION, query, jsonObjectOf()
    )
    return Triple(currentDay, query, foundPomodoroCount)
  }
}
