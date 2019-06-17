package io.acari

import io.reactivex.Single
import io.vertx.config.ConfigRetriever
import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Future
import io.vertx.core.Verticle
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.SingleHelper

class DeploymentVerticle : AbstractVerticle() {

  override fun start(startFuture: Future<Void>) {
    SingleHelper.toSingle<JsonObject> {
      ConfigRetriever.create(vertx).getConfig(it)
    }.flatMap { config ->
      deployVerticle(HttpVerticle(), config)
    }
      .subscribe({
        startFuture.complete()
      }) {
        startFuture.fail(it)
      }
  }

  private fun deployVerticle(
    verticleToDeploy: Verticle,
    config: JsonObject
  ): Single<JsonObject>? {
    return SingleHelper.toSingle<String> {
      vertx.deployVerticle(verticleToDeploy, DeploymentOptions().setConfig(config), it)
    }.map {
      config
    }
  }
}
