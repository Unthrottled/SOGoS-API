package io.acari.memory

import io.acari.memory.user.Effect
import io.acari.memory.user.UserMemoryWorkers
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient

class EffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    mongoClient.rxInsert(EffectSchema.COLLECTION, JsonObject.mapFrom(effect))
      .subscribe({}) {
        UserMemoryWorkers.log.warn("Unable to save user for reasons.", it)
      }
  }
}
