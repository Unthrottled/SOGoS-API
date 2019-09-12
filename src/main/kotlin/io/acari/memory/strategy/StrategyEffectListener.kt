package io.acari.memory.strategy

import com.google.common.collect.Lists
import io.acari.http.CREATED_OBJECTIVE
import io.acari.http.UPDATED_OBJECTIVE
import io.acari.memory.CurrentObjectiveSchema
import io.acari.memory.Effect
import io.acari.memory.ObjectiveHistorySchema
import io.acari.memory.user.UserMemoryWorkers
import io.acari.types.Objective
import io.acari.util.toMaybe
import io.acari.util.toSingletonList
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

class StrategyEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    effect.toMaybe()
      .filter { isObjective(it) }
      .flatMap { writeCurrentObjective(it) }
      .flatMapCompletable { objective ->
          createOrUpdateObjective(mongoClient, objective, effect.guid)
      }
      .subscribe({}) {
        UserMemoryWorkers.log.warn("Unable to save objective for reasons.", it)
      }
  }

  private fun isUpdate(effect: Effect): Boolean {
    return effect.name == UPDATED_OBJECTIVE
  }


  private fun writeCurrentObjective(objectiveEffect: Effect): Maybe<JsonObject>? {
    val objectiveContent = objectiveEffect.content
    val objective = objectiveContent.mapTo(Objective::class.java)
    return mongoClient.rxFindOne(
      CurrentObjectiveSchema.COLLECTION,
      jsonObjectOf(CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER to objectiveEffect.guid),
      jsonObjectOf()
    )
      .flatMap { objectives ->
        val objectiveIds = objectives.getJsonArray(CurrentObjectiveSchema.OBJECTIVES)
        if (objectiveIds.contains(objective.id)) {
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
          CurrentObjectiveSchema.OBJECTIVES to JsonArray(listOf(objective.id))
        )
        mongoClient.rxInsert(CurrentObjectiveSchema.COLLECTION, objectiveEntry)
          .subscribe({ emitter.onSuccess(objectiveContent) }, { emitter.onError(it) }) { emitter.onComplete() }
      })
      .map { it.put(CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER, objectiveEffect.guid) }
  }

  private fun getNewList(objectiveIds: JsonArray, objective: Objective): JsonArray {
    return if (objectiveIds.size() >= MAX_OBJECTIVES) {
      JsonArray(objectiveIds.stream().skip(1).collect({
        Lists.newArrayList<Any>(objective.id.toSingletonList())
      }, { list, item -> list.add(item) },
        { list, otherList -> list.addAll(otherList) })
      )
    } else {
      objectiveIds.add(objective.id)
    }
  }

  private fun isObjective(effect: Effect) =
    effect.name == CREATED_OBJECTIVE ||
      isUpdate(effect)
}

const val MAX_OBJECTIVES = 5


fun createOrUpdateObjective(
  mongoClient: MongoClient,
  activity: JsonObject,
  guid: String
): CompletableSource {
  return mongoClient.rxReplaceDocumentsWithOptions(
    ObjectiveHistorySchema.COLLECTION,
    jsonObjectOf(
      ObjectiveHistorySchema.IDENTIFIER to activity.getString(ObjectiveHistorySchema.IDENTIFIER),
      ObjectiveHistorySchema.GLOBAL_USER_IDENTIFIER to guid
    ),
    activity, UpdateOptions(true)
  ).ignoreElement()
}
