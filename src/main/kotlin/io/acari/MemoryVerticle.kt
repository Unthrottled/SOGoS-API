package io.acari

import io.acari.memory.MemoryInitializations
import io.acari.util.loggerFor
import io.vertx.core.Future
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.mongo.MongoClient

class MemoryVerticle : AbstractVerticle() {
  companion object {
    private val logger = loggerFor(MemoryVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    val memoryConfiguration = config().getJsonObject("memory")
    val mongoClient = MongoClient.createShared(vertx, memoryConfiguration)
    MemoryInitializations.setUpCollections(mongoClient)
      .andThen(MemoryInitializations.registerMemoryWorkers(vertx, mongoClient))
      .subscribe({
        startFuture.complete()
      }){
        startFuture.fail(it)
      }
  }
}
