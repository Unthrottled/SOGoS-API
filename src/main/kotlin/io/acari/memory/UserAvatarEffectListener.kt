package io.acari.memory

import io.acari.http.AVATAR_UPLOADED
import io.acari.http.AVATAR_UPLOAD_REQUESTED
import io.acari.memory.user.UserMemoryWorkers
import io.acari.util.toOptional
import io.vertx.core.Handler
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.eventbus.Message
import io.vertx.reactivex.ext.mongo.MongoClient
import java.util.*

const val AVATAR_UPLOADED_FIELD = "avatarUploaded"
class UserAvatarEffectListener(private val mongoClient: MongoClient, private val vertx: Vertx) :
  Handler<Message<Effect>> {
  companion object {
    private val miscUserMappings =
      mapOf(
        AVATAR_UPLOADED to true,
        AVATAR_UPLOAD_REQUESTED to true
      )
  }

  override fun handle(message: Message<Effect>) {
    val effect = message.body()
    extractUpdateType(effect)
      .ifPresent {
        mongoClient.rxFindOneAndUpdate(
            UserSchema.COLLECTION,
            jsonObjectOf(UserSchema.GLOBAL_USER_IDENTIFIER to effect.guid),
            jsonObjectOf(
              "\$set" to jsonObjectOf(
                "misc.$AVATAR_UPLOADED_FIELD" to true
              )
            )
          )
          .subscribe({}) {
            UserMemoryWorkers.log.warn("Unable to update user avatar attributes for raisins -> ", it)
          }
      }
  }

  private fun extractUpdateType(effect: Effect): Optional<Boolean> {
    return effect.toOptional()
      .filter { miscUserMappings.containsKey(effect.name) }
      .map {
        miscUserMappings[effect.name]
      }
  }
}
