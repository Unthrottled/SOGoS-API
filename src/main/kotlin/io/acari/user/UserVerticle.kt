package io.acari.user

import io.acari.util.loggerFor
import io.vertx.core.Future
import io.vertx.reactivex.core.AbstractVerticle

class UserVerticle : AbstractVerticle() {
  companion object {
    private val logger = loggerFor(UserVerticle::class.java)
  }

  override fun start(startFuture: Future<Void>) {
    startFuture.complete()
  }
}
