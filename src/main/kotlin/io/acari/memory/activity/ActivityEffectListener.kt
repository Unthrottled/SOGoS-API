package io.acari.memory.activity

import io.acari.http.STARTED_ACTIVITY
import io.acari.memory.CurrentActivitySchema
import io.acari.memory.Effect
import io.acari.memory.user.UserMemoryWorkers
import io.reactivex.Completable
import io.reactivex.CompletableSource
import io.vertx.core.Handler
import io.vertx.ext.mongo.UpdateOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient

class ActivityEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    writeCurrentActivity(effect)
      .andThen(writeActivityLog(effect))
      .subscribe({}) {
        UserMemoryWorkers.log.warn("Unable to save user for reasons.", it)
      }
  }

  private fun writeActivityLog(effect: Effect): CompletableSource {
    return Completable.complete()
  }

  private fun writeCurrentActivity(effect: Effect): Completable =
    if (isActivity(effect) && shouldTime(effect)) {
      mongoClient.rxReplaceDocumentsWithOptions(
        CurrentActivitySchema.COLLECTION,
        jsonObjectOf(CurrentActivitySchema.GLOBAL_USER_IDENTIFIER to effect.guid),
        jsonObjectOf(
          CurrentActivitySchema.GLOBAL_USER_IDENTIFIER to effect.guid,
          CurrentActivitySchema.CONTENT to effect.content,
          CurrentActivitySchema.TIME_OF_ANTECEDENCE to effect.antecedenceTime
        ), UpdateOptions(true)
      )
        .ignoreElement()
    } else {
      Completable.complete()
    }

  private fun shouldTime(effect: Effect): Boolean =
    when (effect.content.getString("type") ?: "PASSIVE") {
      "ACTIVE" -> true
      else -> false
    }

  private fun isActivity(effect: Effect) =
    effect.name == STARTED_ACTIVITY
}
