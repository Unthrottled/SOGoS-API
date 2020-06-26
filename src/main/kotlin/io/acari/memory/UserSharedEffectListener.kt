package io.acari.memory

import io.acari.http.DISABLED_SHARED_DASHBOARD
import io.acari.http.ENABLED_SHARED_DASHBOARD
import io.acari.memory.user.UserMemoryWorkers
import io.acari.util.toOptional
import io.vertx.core.Handler
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient
import java.time.Instant
import java.util.*

const val HAS_SHARED_DASHBOARD = "hasShared"
const val SHARED_BRIDGE_CODE = "shareCode"

class UserSharedEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  companion object {
    private val miscUserMappings =
      mapOf(
        ENABLED_SHARED_DASHBOARD to true,
        DISABLED_SHARED_DASHBOARD to false
      )
  }

  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    extractUpdateType(effect)
      .ifPresent { sharedValue ->
        mongoClient.rxFindOneAndUpdate(
          UserSchema.COLLECTION,
          jsonObjectOf(UserSchema.GLOBAL_USER_IDENTIFIER to effect.guid),
          jsonObjectOf(
            "\$set" to jsonObjectOf(
              "security.$HAS_SHARED_DASHBOARD" to sharedValue,
              "security.$SHARED_BRIDGE_CODE" to createShareCode(effect.guid),
              "profile" to message.body().content
            )
          )
        )
          .subscribe({}) {
            UserMemoryWorkers.log.warn("Unable to update user security attributes for raisins -> ", it)
          }
      }
  }

  private fun createShareCode(guid: String): String =
    "${Instant.now().toEpochMilli().toString(36)}${guid.substringBefore('-')}"

  private fun extractUpdateType(effect: Effect): Optional<Boolean> {
    return effect.toOptional()
      .filter { miscUserMappings.containsKey(effect.name) }
      .map {
        miscUserMappings[effect.name]
      }
  }
}
