package io.acari

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future

class DeploymentVerticle : AbstractVerticle() {

  override fun start(startFuture: Future<Void>) {
    vertx.deployVerticle(HttpVerticle()) {
      if (it.succeeded()) {
        startFuture.complete()
      } else {
        startFuture.fail(it.cause())
      }
    }
  }
}
