package io.acari.memory

import io.acari.memory.activity.CurrentActivityRequest
import io.acari.memory.activity.CurrentActivityResponse
import io.acari.memory.activity.activityFromJson
import io.acari.memory.user.UserInfoRequest
import io.acari.memory.user.UserInfoResponse
import io.acari.util.POKOCodec
import io.reactivex.Completable
import io.vertx.core.json.JsonObject
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
      UserInfoRequest::class.java,
      POKOCodec(
        { json, testObject ->
          json.put("openIDInformation", testObject.openIDInformation)
        },
        { jsonObject ->
          UserInfoRequest(jsonObject.getJsonObject("openIDInformation"))
        },
        UserInfoRequest::class.java.name
      )
    )
    eventBus.delegate.registerDefaultCodec(
      CurrentActivityRequest::class.java,
      POKOCodec(
        { json, testObject ->
          json.put("guid", testObject.guid)
        },
        { jsonObject ->
          CurrentActivityRequest(jsonObject.getString("guid"))
        },
        CurrentActivityRequest::class.java.name
      )
    )
    eventBus.delegate.registerDefaultCodec(
      UserInfoResponse::class.java,
      POKOCodec(
        { json, testObject ->
          json.put(UserSchema.GLOBAL_USER_IDENTIFIER, testObject.guid)
        },
        { jsonObject ->
          UserInfoResponse(jsonObject.getString(UserSchema.GLOBAL_USER_IDENTIFIER))
        },
        UserInfoResponse::class.java.name
      )
    )
    eventBus.delegate.registerDefaultCodec(
      CurrentActivityResponse::class.java,
      POKOCodec(
        { json, testObject ->
          json.put("activity", JsonObject.mapFrom(testObject.activity))//todo: just uso jsonObject map from ._.
        },
        { jsonObject ->
          CurrentActivityResponse(activityFromJson(jsonObject))
        },
        CurrentActivityResponse::class.java.name
      )
    )
    eventBus.delegate.registerDefaultCodec(
      Effect::class.java,
      POKOCodec(
        { json, testObject ->
          json.put(UserSchema.GLOBAL_USER_IDENTIFIER, testObject.guid)
            .put(UserSchema.TIME_CREATED, testObject.timeCreated)
        },
        { jsonObject ->
          Effect(jsonObject.getString(EffectSchema.GLOBAL_USER_IDENTIFIER),
            jsonObject.getLong(EffectSchema.TIME_CREATED),
            jsonObject.getLong(EffectSchema.TIME_OF_ANTECEDENCE),
            jsonObject.getString(EffectSchema.NAME),
            jsonObject.getJsonObject(EffectSchema.CONTENT),
            jsonObject.getJsonObject(EffectSchema.META))
        },
        Effect::class.java.name
      )
    )
  }
}
