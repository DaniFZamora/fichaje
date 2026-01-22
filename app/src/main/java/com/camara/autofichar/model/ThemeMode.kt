package com.camara.autofichar.model

enum class ThemeMode(val id: String) {
  AUTO("auto"),
  DARK("dark"),
  LIGHT("light");

  companion object {
    fun fromId(id: String?): ThemeMode {
      return values().firstOrNull { it.id == id } ?: AUTO
    }
  }
}
