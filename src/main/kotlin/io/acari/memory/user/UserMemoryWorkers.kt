package io.acari.memory.user

import io.acari.model.TestObject
import io.acari.util.POKOCodec
import io.reactivex.Completable
import io.vertx.core.Handler
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.EventBus
import java.util.*

data class UserInfoRequest(val userIdentifier: String)
data class UserInfoResponse(val guid: String)

const val USER_INFORMATION_CHANNEL = "user.information"

object UserMemoryWorkers {


  fun registerWorkers(vertx: Vertx): Completable {
    val eventBus = vertx.eventBus()
    registerCodecs(eventBus)
      eventBus.consumer<UserInfoRequest>(USER_INFORMATION_CHANNEL){
          message ->
        message.reply(UserInfoResponse(UUID.randomUUID().toString()))
      }
    return Completable.complete()
  }

  private fun registerCodecs(eventBus: EventBus) {
    eventBus.delegate.registerDefaultCodec(UserInfoRequest::class.java,
      POKOCodec(
        { json, testObject ->
          json.put("userIdentifier", testObject.userIdentifier)
        },
        { jsonObject ->
          UserInfoRequest(jsonObject.getString("userIdentifier"))
        },
        UserInfoRequest::class.java.name
      )
    )
    eventBus.delegate.registerDefaultCodec(UserInfoResponse::class.java,
      POKOCodec(
        { json, testObject ->
          json.put("guid", testObject.guid)
        },
        { jsonObject ->
          UserInfoResponse(jsonObject.getString("guid"))
        },
        UserInfoResponse::class.java.name
      )
    )

  }

}
