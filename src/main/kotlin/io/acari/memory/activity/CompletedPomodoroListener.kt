package io.acari.memory.activity

import io.acari.memory.Effect
import io.acari.util.loggerFor
import io.acari.util.toSingle
import io.reactivex.Completable
import io.vertx.core.Handler
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient

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
    return mongoClient.rxFind("somethnig", jsonObjectOf(
        "something" to completionEffect.guid
    )
    ).ignoreElement()
  }
}
