package com.camara.autofichar.model

enum class DayMode(val id: String) {
  ALL("all"),
  WEEKDAYS("weekdays"),
  CUSTOM("custom");

  companion object {
    fun fromId(id: String?): DayMode {
      return values().firstOrNull { it.id == id } ?: WEEKDAYS
    }
  }
}
