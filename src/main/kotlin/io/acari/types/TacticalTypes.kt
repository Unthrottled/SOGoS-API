package io.acari.types

data class PomodoroSettings(
  val loadDuration: Long,
  val shortRecoveryDuration: Long,
  val longRecoveryDuration: Long
)
