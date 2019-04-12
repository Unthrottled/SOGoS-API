package io.acari.util

import io.vertx.core.logging.LoggerFactory

fun <T> loggerFor(javaClass: Class<T>) = LoggerFactory.getLogger(javaClass)

fun loggerFor(name: String) = LoggerFactory.getLogger(name)
