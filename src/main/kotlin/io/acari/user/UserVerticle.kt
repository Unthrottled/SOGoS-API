package io.acari.user


import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.memory.user.Effect
import io.acari.memory.user.USER_CREATED
import io.acari.util.loggerFor
import io.vertx.core.Future
import io.vertx.reactivex.core.AbstractVerticle

class UserVerticle : AbstractVerticle() {
  companion object {
    private val logger = loggerFor(UserVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    vertx.eventBus().consumer<Effect>(EFFECT_CHANNEL) {
      when(it.body().name) {
        USER_CREATED -> println("I got this ${it.body()}")
      }
    }
    startFuture.complete()
  }
}
