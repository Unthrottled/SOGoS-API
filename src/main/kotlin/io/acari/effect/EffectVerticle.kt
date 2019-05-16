package io.acari.effect


import io.acari.memory.user.EFFECT_CHANNEL
import io.acari.memory.user.Effect
import io.acari.util.loggerFor
import io.vertx.core.Future
import io.vertx.reactivex.core.AbstractVerticle

class EffectVerticle : AbstractVerticle() {
  companion object {
    private val logger = loggerFor(EffectVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    vertx.eventBus().consumer<Effect>(EFFECT_CHANNEL) {
      println("I got this ${it.body()}")
    }
    startFuture.complete()
  }
}
