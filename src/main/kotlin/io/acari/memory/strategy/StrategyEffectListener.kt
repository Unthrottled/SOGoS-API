package io.acari.memory.strategy

import io.acari.http.CREATED_OBJECTIVE
import io.acari.http.UPDATED_OBJECTIVE
import io.acari.memory.*
import io.acari.memory.user.UserMemoryWorkers
import io.reactivex.CompletableSource
import io.reactivex.Single
import io.vertx.core.Handler
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
    writeObjective(effect)
      .flatMapCompletable { activity -> writeActivityLog(activity) }
      .subscribe({}) {
        UserMemoryWorkers.log.warn("Unable to save user for reasons.", it)
      }
  }

  private fun writeActivityLog(activity: JsonObject): CompletableSource {
    return mongoClient.rxInsert(ObjectiveHistorySchema.COLLECTION, activity)
      .ignoreElement()
  }

  private fun writeObjective(effect: Effect): Single<JsonObject> {
    val objective = jsonObjectOf(
      CurrentActivitySchema.GLOBAL_USER_IDENTIFIER to effect.guid,
      CurrentActivitySchema.CONTENT to effect.content,
      CurrentActivitySchema.TIME_OF_ANTECEDENCE to effect.antecedenceTime
    )
    return if (isObjective(effect)) {
      mongoClient.rxReplaceDocumentsWithOptions(
        CurrentActivitySchema.COLLECTION,
        jsonObjectOf(ObjectiveSchema.GLOBAL_USER_IDENTIFIER to effect.guid),
        objective, UpdateOptions(true)
      ).map { objective }
    } else {
      Single.just(objective)
    }
  }

  private fun isObjective(effect: Effect) =
    effect.name == CREATED_OBJECTIVE ||
      effect.name == UPDATED_OBJECTIVE
}
