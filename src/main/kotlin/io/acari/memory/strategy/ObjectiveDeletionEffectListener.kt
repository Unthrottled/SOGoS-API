package io.acari.memory.strategy

import io.acari.http.COMPLETED_OBJECTIVE
import io.acari.http.REMOVED_OBJECTIVE
import io.acari.memory.CurrentObjectiveSchema
import io.acari.memory.Effect
import io.acari.memory.user.UserMemoryWorkers
import io.acari.types.ObjectiveLite
import io.acari.util.toMaybe
import io.reactivex.CompletableSource
import io.reactivex.Maybe
import io.reactivex.MaybeEmitter
import io.vertx.core.Handler
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.mongo.UpdateOptions
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient
import kotlin.streams.toList

class ObjectiveDeletionEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    effect.toMaybe()
      .filter { isObjective(it) }
      .flatMap { removeObjectiveFromCurrentList(it) }
      .flatMapCompletable { updateHistory(it, effect) }
      .subscribe({}) {
        UserMemoryWorkers.log.warn("Unable to remove objective for reasons.", it)
      }
  }

  private fun updateHistory(objective: JsonObject, objectiveEffect: Effect): CompletableSource =
    createOrUpdateObjective(mongoClient, modifyObjective(objective, objectiveEffect), objectiveEffect.guid)

  private fun modifyObjective(objective: JsonObject, objectiveEffect: Effect): JsonObject =
    if(isCompletion(objectiveEffect)) {
      objective.put("completionTime", objectiveEffect.antecedenceTime)
    } else {
      objective.put("removalTime", objectiveEffect.antecedenceTime)
    }

  private fun removeObjectiveFromCurrentList(objectiveEffect: Effect): Maybe<JsonObject>? {
    val objectiveContent = objectiveEffect.content
    val objective = objectiveContent.mapTo(ObjectiveLite::class.java)
    return mongoClient.rxFindOne(
      CurrentObjectiveSchema.COLLECTION,
      jsonObjectOf(CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER to objectiveEffect.guid),
      jsonObjectOf()
    )
      .flatMap { objectives ->
        val objectiveIds = objectives.getJsonArray(CurrentObjectiveSchema.OBJECTIVES)
        if (!objectiveIds.contains(objective.id)) {
          Maybe.just(objectiveContent)
        } else {
          objectives.put(CurrentObjectiveSchema.OBJECTIVES, getNewList(objectiveIds, objective))
          mongoClient.rxReplaceDocumentsWithOptions(
            CurrentObjectiveSchema.COLLECTION,
            jsonObjectOf(CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER to objectiveEffect.guid),
            objectives, UpdateOptions(true)
          ).toMaybe().map { objectiveContent }
        }
      }
      .switchIfEmpty(Maybe.create { emitter: MaybeEmitter<JsonObject> ->
        val objectiveEntry = jsonObjectOf(
          CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER to objectiveEffect.guid,
          CurrentObjectiveSchema.OBJECTIVES to JsonArray()
        )
        mongoClient.rxInsert(CurrentObjectiveSchema.COLLECTION, objectiveEntry)
          .subscribe({ emitter.onSuccess(objectiveContent) }, { emitter.onError(it) }) { emitter.onComplete() }
      })
      .map { it.put(CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER, objectiveEffect.guid) }
  }

  private fun getNewList(objectiveIds: JsonArray, objective: ObjectiveLite): JsonArray =
    JsonArray(objectiveIds.stream()
      .filter { it != objective.id }
      .toList())

  private fun isObjective(effect: Effect) =
    isRemoval(effect) || isCompletion(effect)

  private fun isRemoval(effect: Effect): Boolean = effect.name == REMOVED_OBJECTIVE

  private fun isCompletion(effect: Effect): Boolean = effect.name == COMPLETED_OBJECTIVE
}

