package io.acari.memory.strategy

import io.acari.http.CREATED_OBJECTIVE
import io.acari.http.UPDATED_OBJECTIVE
import io.acari.memory.CurrentObjectiveSchema
import io.acari.memory.Effect
import io.acari.memory.ObjectiveHistorySchema
import io.acari.memory.user.UserMemoryWorkers
import io.acari.types.Objective
import io.acari.util.toSingle
import io.reactivex.CompletableSource
import io.reactivex.Maybe
import io.reactivex.MaybeEmitter
import io.reactivex.Single
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.UpdateOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient

class StrategyEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    effect.toSingle()
      .filter { isObjective(it) }
      .flatMapSingle { writeObjective(it) }
      .flatMapCompletable { activity -> writeActivityLog(activity) }
      .subscribe({}) {
        UserMemoryWorkers.log.warn("Unable to save objective for reasons.", it)
      }
  }

  private fun writeActivityLog(activity: JsonObject): CompletableSource {
    return mongoClient.rxInsert(ObjectiveHistorySchema.COLLECTION, activity)
      .ignoreElement()
  }

  private fun writeObjective(objectiveEffect: Effect): Single<JsonObject> {
    val objectiveContent = objectiveEffect.content
    val objective = objectiveContent.mapTo(Objective::class.java)
    return mongoClient.rxFindOne(
      CurrentObjectiveSchema.COLLECTION,
      jsonObjectOf(CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER to objectiveEffect.guid),
      jsonObjectOf())
      .flatMap {objectives ->
        mongoClient.rxReplaceDocumentsWithOptions(
          CurrentObjectiveSchema.COLLECTION,
          jsonObjectOf(CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER to objectiveEffect.guid),
          objectives, UpdateOptions(true)
        ).toMaybe().map { objectiveContent }
      }
      .switchIfEmpty(Maybe.create { emitter: MaybeEmitter<JsonObject> ->
        val objectiveEntry = jsonObjectOf(
          CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER to objectiveEffect.guid,
          CurrentObjectiveSchema.OBJECTIVES to JsonArray(listOf(objectiveContent))
        )
        mongoClient.rxInsert(CurrentObjectiveSchema.COLLECTION, objectiveEntry)
          .subscribe({ emitter.onSuccess(objectiveContent) }, { emitter.onError(it) }) { emitter.onComplete() }
      }).toSingle()

  }

  private fun isObjective(effect: Effect) =
    effect.name == CREATED_OBJECTIVE ||
      effect.name == UPDATED_OBJECTIVE
}
