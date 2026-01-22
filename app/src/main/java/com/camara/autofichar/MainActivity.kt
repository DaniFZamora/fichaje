package com.camara.autofichar

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.camara.autofichar.data.SettingsRepository
import com.camara.autofichar.ui.AppNav
import com.camara.autofichar.ui.theme.CamaraTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val requestPostNotif = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val settingsRepo = SettingsRepository(applicationContext)

    setContent {
      val settings by settingsRepo.settingsFlow.collectAsState(initial = null)

      val s = settings
      if (s != null) {
        CamaraTheme(themeMode = s.theme) {
          AppNav(
            settings = s,
            settingsRepo = settingsRepo,
            onRequestExactAlarm = { requestExactAlarmPermission() },
            onRequestIgnoreBatteryOpt = { requestIgnoreBatteryOptimizations() },
            onRequestNotifications = { requestNotifPermissionIfNeeded() }
          )
        }
      } else {
        androidx.compose.material3.MaterialTheme {
          androidx.compose.material3.Surface {
            androidx.compose.material3.Text("Cargando…")
          }
        }
      }
    }
  }

  private fun requestNotifPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= 33) {
      requestPostNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  private fun requestExactAlarmPermission() {
    if (Build.VERSION.SDK_INT < 31) return
    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (alarmManager.canScheduleExactAlarms()) return

    try {
      startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
    } catch (_: Exception) {
      // fallback: app details
      val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
      }
      startActivity(i)
    }
  }

  private fun requestIgnoreBatteryOptimizations() {
    if (Build.VERSION.SDK_INT < 23) return
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    if (pm.isIgnoringBatteryOptimizations(packageName)) return

    try {
      val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:$packageName")
      }
      startActivity(i)
    } catch (_: Exception) {
      val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
      }
      startActivity(i)
    }
  }

}
