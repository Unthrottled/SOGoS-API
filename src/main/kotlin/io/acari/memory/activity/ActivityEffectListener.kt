package io.acari.memory.activity

import io.acari.http.STARTED_ACTIVITY
import io.acari.memory.ActivityHistorySchema
import io.acari.memory.CurrentActivitySchema
import io.acari.memory.Effect
import io.acari.memory.UserSchema
import io.acari.memory.user.UserMemoryWorkers
import io.acari.util.toOptional
import io.reactivex.CompletableSource
import io.reactivex.Maybe
import io.reactivex.MaybeObserver
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.UpdateOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient
import java.util.*

const val CURRENT_ACTIVITY_CHANNEL = "CURRENT_ACTIVITY_CHANNEL"

class ActivityEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    writeCurrentActivity(effect)
      .flatMapCompletable { activity -> writeActivityLog(activity) }
      .subscribe({}) {
        UserMemoryWorkers.log.warn("Unable to save user for reasons.", it)
      }
  }

  private fun writeActivityLog(activity: JsonObject): CompletableSource {
    return mongoClient.rxInsert(ActivityHistorySchema.COLLECTION, activity)
      .ignoreElement()
  }

  private fun writeCurrentActivity(effect: Effect): Maybe<JsonObject> {
    val activity = jsonObjectOf(
      CurrentActivitySchema.GLOBAL_USER_IDENTIFIER to effect.guid,
      CurrentActivitySchema.CONTENT to effect.content,
      CurrentActivitySchema.TIME_OF_ANTECEDENCE to effect.antecedenceTime
    )
    val isActivity = isActivity(effect)
    return when {
      isActivity && shouldTime(effect) -> updateCurrentActivity(effect, activity, vertx)
      isActivity -> Maybe.just(activity)
      else -> Maybe.empty()
    }
  }

  private fun updateCurrentActivity(
    effect: Effect,
    activity: JsonObject,
    vertx: Vertx
  ): Maybe<JsonObject> {
    val userIdentifier = effect.guid
    return findCurrentActivity(mongoClient, userIdentifier)
      .map {
        it.getJsonObject(CurrentActivitySchema.CURRENT).toOptional()
      }
      .switchIfEmpty { observer: MaybeObserver<in Optional<JsonObject>> -> observer.onSuccess(Optional.empty()) }
      .flatMapSingle { previousActivity ->
        val baseJson = jsonObjectOf(
          CurrentActivitySchema.CURRENT to activity,
          CurrentActivitySchema.GLOBAL_USER_IDENTIFIER to userIdentifier
        )
        val activityScope = previousActivity.map {
          baseJson.put(CurrentActivitySchema.PREVIOUS, it)
        }.orElseGet {
          baseJson
        }
        mongoClient.rxReplaceDocumentsWithOptions(
          CurrentActivitySchema.COLLECTION,
          jsonObjectOf(CurrentActivitySchema.GLOBAL_USER_IDENTIFIER to userIdentifier),
          activityScope, UpdateOptions(true)
        ).doOnSuccess {
          vertx.eventBus()
            .publish(
              CURRENT_ACTIVITY_CHANNEL,
              activityScope.put(UserSchema.GLOBAL_USER_IDENTIFIER, userIdentifier)
            )
        }
      }.map { activity }
      .toMaybe()
  }

  private fun shouldTime(effect: Effect): Boolean =
    when (effect.content.getString("type") ?: "PASSIVE") {
      "ACTIVE" -> true
      else -> false
    }

  private fun isActivity(effect: Effect) =
    effect.name == STARTED_ACTIVITY
}
