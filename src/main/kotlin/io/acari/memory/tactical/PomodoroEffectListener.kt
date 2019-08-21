package io.acari.memory.tactical

import io.acari.http.UPDATED_POMODORO_SETTINGS
import io.acari.memory.CurrentObjectiveSchema
import io.acari.memory.Effect
import io.acari.memory.TacticalSettingsSchema
import io.acari.memory.user.UserMemoryWorkers
import io.acari.types.PomodoroSettings
import io.acari.util.toMaybe
import io.reactivex.Completable
import io.vertx.core.Handler
import io.vertx.ext.mongo.UpdateOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient

class PomodoroEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    effect.toMaybe()
      .filter { isPomodoro(it) }
      .flatMapCompletable { writePomodoroSettings(it) }
      .subscribe({}) {
        UserMemoryWorkers.log.warn("Unable to save tactical settings for reasons.", it)
      }
  }

  private fun writePomodoroSettings(pomodoroEffect: Effect): Completable {
    val pomodoroContent = pomodoroEffect.content
    return mongoClient.rxReplaceDocumentsWithOptions(
      TacticalSettingsSchema.COLLECTION,
      jsonObjectOf(TacticalSettingsSchema.GLOBAL_USER_IDENTIFIER to pomodoroEffect.guid),
      jsonObjectOf(TacticalSettingsSchema.POMODORO_SETTINGS to pomodoroContent),
      UpdateOptions(true)
    ).toCompletable()
  }

  private fun isPomodoro(effect: Effect) =
    effect.name == UPDATED_POMODORO_SETTINGS
}

