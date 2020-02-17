package io.acari.memory

import io.acari.http.USER_WELCOMED
import io.acari.memory.user.UserMemoryWorkers
import io.acari.util.toOptional
import io.vertx.core.Handler
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient
import java.util.*

class SOGoSUserEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  companion object {
    private val miscUserMappings =
      mapOf(
        USER_WELCOMED to "welcomed"
      )
  }


  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    extractUpdateType(effect)
      .ifPresent { updateType ->
        mongoClient.rxFindOneAndUpdate(
          UserSchema.COLLECTION,
          jsonObjectOf(UserSchema.GLOBAL_USER_IDENTIFIER to effect.guid),
          jsonObjectOf(
            "\$set" to jsonObjectOf(
              "misc.$updateType" to true
            )
          )
        )
          .subscribe({}) {
            UserMemoryWorkers.log.warn("Unable to update misc user attributes for raisins -> ", it)
          }
      }
  }

  private fun extractUpdateType(effect: Effect): Optional<String> {
    return effect.toOptional()
      .filter { miscUserMappings.containsKey(effect.name) }
      .map {
        miscUserMappings[effect.name]
      }
  }
}
