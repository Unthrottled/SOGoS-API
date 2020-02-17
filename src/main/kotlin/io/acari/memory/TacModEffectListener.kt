package io.acari.memory

import io.acari.http.TACMOD_DOWNLOADED
import io.acari.http.TACMOD_NOTIFIED
import io.acari.http.TACMOD_THANKED
import io.acari.memory.user.UserMemoryWorkers
import io.acari.util.toOptional
import io.vertx.core.Handler
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient
import java.util.*


class TacModEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  companion object {
    private val tacModEffectMappings =
      mapOf(
        TACMOD_DOWNLOADED to "TacModDownloaded",
        TACMOD_NOTIFIED to "TacModNotified",
        TACMOD_THANKED to "TacModThanked"
      )
  }


  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    extractUpdateType(effect)
      .ifPresent {
        updateType ->
        mongoClient.rxFindOneAndUpdate(
          UserSchema.COLLECTION,
          jsonObjectOf(UserSchema.GLOBAL_USER_IDENTIFIER to effect.guid),
          jsonObjectOf(
            "\$set" to jsonObjectOf(
              "misc.onboarding.$updateType" to true
            )
          )
        )
          .subscribe({}) {
            UserMemoryWorkers.log.warn("Unable to update misc TacMod user attributes for raisins -> ", it)
          }
      }
  }

  private fun extractUpdateType(effect: Effect): Optional<String> {
    return effect.toOptional()
      .filter { tacModEffectMappings.containsKey(effect.name)  }
      .map {
        tacModEffectMappings[effect.name]
      }
  }
}
