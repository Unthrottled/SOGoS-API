package io.acari

import io.acari.util.loggerFor
import io.reactivex.Completable
import io.reactivex.Observer
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
    mongoClient.rxCreateCollection("test")
      .onErrorResumeNext { error->
        if(error.message?.contains("already exists") == true){
          Completable.complete()
        } else {
          Completable.error(error)
        }
      }
      .andThen(mongoClient.rxGetCollections())
      .subscribe({
        startFuture.complete()
      }){
        startFuture.fail(it)
      }
  }
}
