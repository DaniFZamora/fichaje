package com.camara.autofichar.model

data class Settings(
  val enabled: Boolean,
  val dayMode: DayMode,
  val customDays: BooleanArray, // 0=Dom..6=Sab
  val slots: List<Slot>,
  val theme: ThemeMode,
  val nextRuns: List<Long>, // millis per slot (size=4, -1 when none)
  val lastResult: String,
  val lastResultAt: Long
)
