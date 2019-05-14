package io.acari.util

import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.micrometer.micrometerMetricsOptionsOf
import io.vertx.kotlin.micrometer.vertxPrometheusOptionsOf

fun <T> loggerFor(javaClass: Class<T>) = LoggerFactory.getLogger(javaClass)

fun loggerFor(name: String) = LoggerFactory.getLogger(name)

fun fetchConfiguredVertx(): Vertx =
Vertx.vertx(
  vertxOptionsOf(
    metricsOptions = micrometerMetricsOptionsOf(
      enabled = true,
      prometheusOptions = vertxPrometheusOptionsOf(
        enabled = true
      )
    )
  )
)
