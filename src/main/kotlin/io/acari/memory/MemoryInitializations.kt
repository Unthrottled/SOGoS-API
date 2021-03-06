package io.acari.memory

import io.acari.memory.activity.ActivityMemoryWorkers
import io.acari.memory.strategy.StrategyMemoryWorkers
import io.acari.memory.tactical.TacticalMemoryWorkers
import io.acari.memory.user.UserMemoryWorkers
import io.reactivex.Completable
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

object MemoryInitializations {
  fun setUpCollections(mongoClient: MongoClient): Completable =
    createCollection(mongoClient, UserSchema.COLLECTION)
      .andThen(
        mongoClient.rxCreateIndex(
          UserSchema.COLLECTION, jsonObjectOf(
            UserSchema.OAUTH_IDENTIFIERS to 1
          )
        )
      )
      .andThen(createCollection(mongoClient, EffectSchema.COLLECTION))
      .andThen(createCollection(mongoClient, CurrentActivitySchema.COLLECTION))
      .andThen(createCollection(mongoClient, ActivityHistorySchema.COLLECTION))
      .andThen(createCollection(mongoClient, CurrentObjectiveSchema.COLLECTION))
      .andThen(createCollection(mongoClient, ObjectiveHistorySchema.COLLECTION))
      .andThen(createCollection(mongoClient, TacticalSettingsSchema.COLLECTION))
      .andThen(createCollection(mongoClient, TacticalActivitySchema.COLLECTION))
      .andThen(createCollection(mongoClient, PomodoroHistorySchema.COLLECTION))

  fun registerCodecs(vertx: Vertx): Completable =
    MemoryCodecs.attachCodecsToEventBus(vertx)

  fun registerMemoryWorkers(vertx: Vertx, mongoClient: MongoClient): Completable =
    UserMemoryWorkers.registerWorkers(vertx, mongoClient)
      .andThen(ActivityMemoryWorkers.registerWorkers(vertx, mongoClient))
      .andThen(StrategyMemoryWorkers.registerWorkers(vertx, mongoClient))
      .andThen(TacticalMemoryWorkers.registerWorkers(vertx, mongoClient))

  private fun createCollection(mongoClient: MongoClient, collectionName: String): Completable =
    mongoClient.rxCreateCollection(collectionName)
      .onErrorResumeNext { error ->
        if (error.message?.contains("already exists") == true) {
          Completable.complete()
        } else {
          Completable.error(error)
        }
      }
}
