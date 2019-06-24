package io.acari.memory

import io.acari.util.POKOCodec
import io.reactivex.Completable
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.EventBus

object MemoryCodecs {

  fun attachCodecsToEventBus(vertx: Vertx): Completable {
    val eventBus = vertx.eventBus()
    registerCodecs(eventBus)
    return Completable.complete()
  }

  private fun registerCodecs(eventBus: EventBus) {
    eventBus.delegate.registerDefaultCodec(
      Effect::class.java,
      POKOCodec(
        { json, testObject ->
          json.put(UserSchema.GLOBAL_USER_IDENTIFIER, testObject.guid)
            .put(UserSchema.TIME_CREATED, testObject.timeCreated)
        },
        { jsonObject ->
          Effect(
            jsonObject.getString(EffectSchema.GLOBAL_USER_IDENTIFIER),
            jsonObject.getLong(EffectSchema.TIME_CREATED),
            jsonObject.getLong(EffectSchema.TIME_OF_ANTECEDENCE),
            jsonObject.getString(EffectSchema.NAME),
            jsonObject.getJsonObject(EffectSchema.CONTENT),
            jsonObject.getJsonObject(EffectSchema.META)
          )
        },
        Effect::class.java.name
      )
    )
  }
}
