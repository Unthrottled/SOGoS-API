package io.acari.types

data class Objective(
  val id: String,
  val valueStatement: String,
  val keyResults: List<KeyResult>,
  val antecedenceTime: Long
)

data class ObjectiveLite(
  val id: String
)

data class KeyResult(
  val id: String,
  val valueStatement: String,
  val antecedenceTime: Long
)
