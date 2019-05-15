package io.acari.saga


import io.acari.memory.user.NEW_USER_CHANNEL
import io.acari.memory.user.UserCreatedEvent
import io.acari.util.loggerFor
import io.vertx.core.Future
import io.vertx.reactivex.core.AbstractVerticle

class SagaVerticle : AbstractVerticle() {
  companion object {
    private val logger = loggerFor(SagaVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    vertx.eventBus().consumer<UserCreatedEvent>(NEW_USER_CHANNEL) {
      println("I got this ${it.body()}")
    }
    startFuture.complete()
  }
}
