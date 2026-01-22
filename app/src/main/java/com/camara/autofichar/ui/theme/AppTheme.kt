package com.camara.autofichar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.camara.autofichar.model.ThemeMode

@Composable
fun CamaraTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
  val dark = when (themeMode) {
    ThemeMode.AUTO -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
  }

  val colors = if (dark) darkColorScheme() else lightColorScheme()

  MaterialTheme(
    colorScheme = colors,
    typography = MaterialTheme.typography,
    content = content
  )
}
