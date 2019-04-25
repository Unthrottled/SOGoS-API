package io.acari

import io.acari.util.loggerFor
import io.vertx.core.Future
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.mongo.MongoClient

class MemoryVerticle : AbstractVerticle() {
  companion object {
    private val logger = loggerFor(MemoryVerticle::class.java)
    init {
//        System.setProperty("org.mongodb.async.type","netty")
    }
  }

  override fun start(startFuture: Future<Void>) {
    val memoryConfiguration = config().getJsonObject("memory")
    val mongoClient = MongoClient.createShared(vertx, memoryConfiguration)
    mongoClient.rxGetCollections()
      .subscribe({
        println(it)
        startFuture.complete()
      }){
        startFuture.fail(it)
      }
  }
}
