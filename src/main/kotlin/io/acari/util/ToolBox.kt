package io.acari.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.logging.LoggerFactory
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.micrometer.micrometerMetricsOptionsOf
import io.vertx.kotlin.micrometer.vertxPrometheusOptionsOf

fun <T> loggerFor(javaClass: Class<T>) = LoggerFactory.getLogger(javaClass)

fun loggerFor(name: String) = LoggerFactory.getLogger(name)

fun fetchConfiguredVertx(): Vertx {
  configureJsonModule()
  return Vertx.vertx(
    vertxOptionsOf(
      metricsOptions = micrometerMetricsOptionsOf(
        enabled = true,
        prometheusOptions = vertxPrometheusOptionsOf(
          enabled = true
        )
      )
    )
  )
}

private fun configureJsonModule() {
  configureMapper(Json.mapper)
  configureMapper(Json.prettyMapper)
}

private fun configureMapper(mapper: ObjectMapper) {
  mapper.apply {
    registerKotlinModule()
  }
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}
