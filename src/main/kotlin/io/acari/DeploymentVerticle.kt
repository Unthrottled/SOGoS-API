package io.acari

import io.vertx.core.Future
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.core.RxHelper.deployVerticle

class DeploymentVerticle : AbstractVerticle() {

  override fun start(startFuture: Future<Void>) {
    deployVerticle(vertx, HttpVerticle())
      .subscribe({
        startFuture.complete()
      }) {
        startFuture.fail(it)
      }
  }

}
