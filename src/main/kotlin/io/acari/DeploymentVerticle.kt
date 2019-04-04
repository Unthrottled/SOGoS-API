package io.acari

import io.vertx.config.ConfigRetriever
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.SingleHelper

class DeploymentVerticle : AbstractVerticle() {

  override fun start(startFuture: Future<Void>) {
    SingleHelper.toSingle<JsonObject> {
      ConfigRetriever.create(vertx).getConfig(it)
    }.flatMap { config ->
      SingleHelper.toSingle<String> {
        vertx.deployVerticle(HttpVerticle(), DeploymentOptions().setConfig(config), it)
      }
    }.subscribe({
        startFuture.complete()
      }) {
        startFuture.fail(it)
      }
  }
}
