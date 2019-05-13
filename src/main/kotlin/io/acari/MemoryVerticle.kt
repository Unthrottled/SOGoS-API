package io.acari

import io.acari.util.loggerFor
import io.reactivex.Completable
import io.reactivex.Observer
import io.vertx.core.Future
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.mongo.MongoClient

class MemoryVerticle : AbstractVerticle() {
  companion object {
    private val logger = loggerFor(MemoryVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    val memoryConfiguration = config().getJsonObject("memory")
    val mongoClient = MongoClient.createShared(vertx, memoryConfiguration)
    createCollection(mongoClient, "test")
      .andThen(createCollection(mongoClient, "user"))
      .andThen(mongoClient.rxCreateIndex("user", jsonObjectOf(
        "identifiers" to 1
      )))
      .andThen(mongoClient.rxGetCollections())
      .subscribe({
        startFuture.complete()
      }){
        startFuture.fail(it)
      }
  }

  fun createCollection(mongoClient: MongoClient, collectionName: String): Completable =
    mongoClient.rxCreateCollection(collectionName)
      .onErrorResumeNext { error->
        if(error.message?.contains("already exists") == true){
          Completable.complete()
        } else {
          Completable.error(error)
        }
      }
}
