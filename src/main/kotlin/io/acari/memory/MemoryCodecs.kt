package io.acari.memory

import io.acari.memory.user.UserCreatedEvent
import io.acari.memory.user.UserInfoRequest
import io.acari.memory.user.UserInfoResponse
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
      UserInfoRequest::class.java,
      POKOCodec(
        { json, testObject ->
          json.put(UserSchema.OAUTH_IDENTIFIERS, testObject.userIdentifier)
        },
        { jsonObject ->
          UserInfoRequest(jsonObject.getString(UserSchema.OAUTH_IDENTIFIERS))
        },
        UserInfoRequest::class.java.name
      )
    )
    eventBus.delegate.registerDefaultCodec(
      UserInfoResponse::class.java,
      POKOCodec(
        { json, testObject ->
          json.put(UserSchema.GLOBAL_IDENTIFIER, testObject.guid)
        },
        { jsonObject ->
          UserInfoResponse(jsonObject.getString(UserSchema.GLOBAL_IDENTIFIER))
        },
        UserInfoResponse::class.java.name
      )
    )
    eventBus.delegate.registerDefaultCodec(
      UserCreatedEvent::class.java,
      POKOCodec(
        { json, testObject ->
          json.put(UserSchema.GLOBAL_IDENTIFIER, testObject.guid)
            .put(UserSchema.TIME_CREATED, testObject.timeCreated)
        },
        { jsonObject ->
          UserCreatedEvent(jsonObject.getString(UserSchema.GLOBAL_IDENTIFIER),
            jsonObject.getLong(UserSchema.TIME_CREATED))
        },
        UserCreatedEvent::class.java.name
      )
    )
  }
}
