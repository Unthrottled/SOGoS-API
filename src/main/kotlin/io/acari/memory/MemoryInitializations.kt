package io.acari.memory

import io.acari.memory.user.UserMemoryWorkers
import io.reactivex.Completable
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.mongo.MongoClient

object MemoryInitializations {
  fun setUpCollections(mongoClient: MongoClient): Completable =
    createCollection(mongoClient, "test")
      .andThen(createCollection(mongoClient, "user"))
      .andThen(mongoClient.rxCreateIndex("user", jsonObjectOf(
        "identifiers" to 1
      )
      ))

  fun registerMemoryWorkers(vertx: Vertx, mongoClient: MongoClient): Completable =
    UserMemoryWorkers.registerWorkers(vertx, mongoClient)

  private fun createCollection(mongoClient: MongoClient, collectionName: String): Completable =
    mongoClient.rxCreateCollection(collectionName)
      .onErrorResumeNext { error->
        if(error.message?.contains("already exists") == true){
          Completable.complete()
        } else {
          Completable.error(error)
        }
      }
}
