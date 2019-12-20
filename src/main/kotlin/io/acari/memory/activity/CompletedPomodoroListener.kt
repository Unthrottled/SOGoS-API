package io.acari.memory.activity

import io.acari.memory.Effect
import io.acari.memory.PomodoroCompletionHistorySchema.COLLECTION
import io.acari.memory.PomodoroCompletionHistorySchema.COUNT
import io.acari.memory.PomodoroCompletionHistorySchema.DAY
import io.acari.memory.PomodoroCompletionHistorySchema.GLOBAL_USER_IDENTIFIER
import io.acari.util.loggerFor
import io.acari.util.toSingle
import io.reactivex.Completable
import io.reactivex.MaybeObserver
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
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

  override fun handle(event: Message<Effect>) {
    event.body().toSingle()
      .filter { it.name == "RECOVERY" }
      .filter { it.content.containsKey("autoStart") }
      .flatMapCompletable { writePomodoroCount(it) }
      .subscribe({

      }) {
        log.error("Unable to process completed pomodoro for event ${event.body()}")
      }
  }

  private fun writePomodoroCount(completionEffect: Effect): Completable {
    val currentDay = Duration.between(Instant.MIN, Instant.now()).toDays()
    return mongoClient.rxFindOne(
      COLLECTION, jsonObjectOf(
        GLOBAL_USER_IDENTIFIER to completionEffect.guid,
        DAY to currentDay
      ), jsonObjectOf()
    )
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
      .flatMap {
        mongoClient.rxInsert(COLLECTION, it)
      }
      .ignoreElement()
  }
}
