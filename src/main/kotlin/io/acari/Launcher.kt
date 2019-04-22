package io.acari

import io.acari.util.fetchConfiguredVertx
import io.acari.util.loggerFor
import io.vertx.reactivex.SingleHelper

val logger = loggerFor("Launcher")

fun main() {
  val configuredVertx = fetchConfiguredVertx()
  SingleHelper.toSingle<String> {
    configuredVertx.deployVerticle(DeploymentVerticle(), it)
  }.subscribe({
    logger.info("Succeeded in launching application! \uD83D\uDE80")
  }){
    logger.fatal("Houston, we have a problem.", it)
  }

}
