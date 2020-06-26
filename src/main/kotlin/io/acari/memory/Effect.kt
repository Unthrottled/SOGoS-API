package io.acari.memory

import io.vertx.core.json.JsonObject

data class Effect(
  val guid: String,
  val timeCreated: Long,
  val antecedenceTime: Long,
  val name: String,
  val content: JsonObject,
  val meta: JsonObject
)
