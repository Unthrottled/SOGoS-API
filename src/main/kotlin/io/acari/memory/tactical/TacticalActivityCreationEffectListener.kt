package io.acari.memory.tactical

import io.acari.http.CREATED_TACTICAL_ACTIVITY
import io.acari.http.UPDATED_TACTICAL_ACTIVITY
import io.acari.memory.Effect
import io.acari.memory.ObjectiveHistorySchema
import io.acari.memory.TacticalActivitySchema
import io.acari.memory.TacticalSettingsSchema
import io.acari.memory.user.UserMemoryWorkers
import io.acari.util.toMaybe
import io.reactivex.Completable
import io.vertx.core.Handler
import io.vertx.ext.mongo.UpdateOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient

class TacticalActivityCreationEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    effect.toMaybe()
      .filter { isTacticalActivity(it) }
      .flatMapCompletable { writeTacticalActivity(it) }
      .subscribe({}) {
        UserMemoryWorkers.log.warn("Unable to save tactical settings for reasons.", it)
      }
  }

  private fun writeTacticalActivity(tacticalActivityEffect: Effect): Completable {
    val activity = tacticalActivityEffect.content
    val guid = tacticalActivityEffect.guid
    activity.put("removed", false)
    activity.put(TacticalActivitySchema.GLOBAL_USER_IDENTIFIER, guid)
    return mongoClient.rxReplaceDocumentsWithOptions(
      TacticalActivitySchema.COLLECTION,
      jsonObjectOf(
        TacticalActivitySchema.IDENTIFIER to activity.getString(TacticalActivitySchema.IDENTIFIER),
        TacticalActivitySchema.GLOBAL_USER_IDENTIFIER to guid
      ),
      activity, UpdateOptions(true)
    ).ignoreElement()
  }

  private fun isTacticalActivity(effect: Effect) =
    effect.name == UPDATED_TACTICAL_ACTIVITY ||
      effect.name == CREATED_TACTICAL_ACTIVITY
}

