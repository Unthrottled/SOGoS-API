package io.acari.memory

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

  fun registerCodecs(vertx: Vertx): Completable =
    MemoryCodecs.attachCodecsToEventBus(vertx)

  fun registerMemoryWorkers(vertx: Vertx, mongoClient: MongoClient): Completable =
    UserMemoryWorkers.registerWorkers(vertx, mongoClient)

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
