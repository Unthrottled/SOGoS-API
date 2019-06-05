package io.acari.memory.activity

import io.acari.http.STARTED_ACTIVITY
import io.acari.memory.ActivitySchema
import io.acari.memory.Effect
import io.acari.memory.user.UserMemoryWorkers
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
    if (isActivity(effect) && shouldTime(effect)) {
      mongoClient.rxReplaceDocumentsWithOptions(
        ActivitySchema.COLLECTION,
        jsonObjectOf(ActivitySchema.GLOBAL_USER_IDENTIFIER to effect.guid),
        jsonObjectOf(
          ActivitySchema.GLOBAL_USER_IDENTIFIER to effect.guid,
          ActivitySchema.CONTENT to effect.content,
          ActivitySchema.TIME_OF_ANTECEDENCE to effect.antecedenceTime
        ), UpdateOptions(true)
      )
        .ignoreElement()
        .subscribe({}) {
          UserMemoryWorkers.log.warn("Unable to save user for reasons.", it)
        }
    }
  }

  private fun shouldTime(effect: Effect): Boolean =
    when (effect.content.getString("type") ?: "PASSIVE") {
      "ACTIVE" -> true
      else -> false
    }

  private fun isActivity(effect: Effect) =
    effect.name == STARTED_ACTIVITY
}
