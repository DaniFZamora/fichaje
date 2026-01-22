package com.camara.autofichar.ui

import android.content.Context
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.camara.autofichar.automation.AutomationService
import com.camara.autofichar.data.*
import com.camara.autofichar.model.Settings
import com.camara.autofichar.scheduler.AlarmScheduler
import kotlinx.coroutines.launch

private object Routes {
  const val HOME = "home"
  const val DAYS = "days"
  const val SCHEDULE = "schedule"
  const val LOGIN = "login"
  const val LOGS = "logs"
  const val ABOUT = "about"
}

@Composable
fun AppNav(
  settings: Settings,
  settingsRepo: SettingsRepository,
  onRequestExactAlarm: () -> Unit,
  onRequestIgnoreBatteryOpt: () -> Unit,
  onRequestNotifications: () -> Unit
) {
  val nav = rememberNavController()
  val ctx = LocalContext.current
  val scope = rememberCoroutineScope()

  val logRepo = remember { LogRepository(ctx.applicationContext) }
  val credsRepo = remember { CredentialsRepository(ctx.applicationContext) }

  val scheduler = remember { AlarmScheduler(ctx.applicationContext, settingsRepo) }

  Scaffold(
    topBar = {
      TopBar(nav)
    }
  ) { padding ->
    NavHost(
      navController = nav,
      startDestination = Routes.HOME,
      modifier = androidx.compose.ui.Modifier.padding(padding)
    ) {
      composable(Routes.HOME) {
        HomeScreen(
          settings = settings,
          onGoDays = { nav.navigate(Routes.DAYS) },
          onGoSchedule = { nav.navigate(Routes.SCHEDULE) },
          onGoLogin = { nav.navigate(Routes.LOGIN) },
          onGoLogs = { nav.navigate(Routes.LOGS) },
          onGoAbout = { nav.navigate(Routes.ABOUT) },
          onToggleEnabled = { enabled ->
            scope.launch {
              settingsRepo.setEnabled(enabled)
              scheduler.rescheduleAll(reason = "toggle")
            }
          },
          onRunNow = {
            // Dispara servicio manual
            val i = Intent(ctx, AutomationService::class.java).apply {
              putExtra(AutomationService.EXTRA_SLOT_INDEX, -1)
              putExtra(AutomationService.EXTRA_TRIGGER, "manual")
            }
            ctx.startForegroundService(i)
          },
          onReschedule = { scope.launch { scheduler.rescheduleAll(reason = "manual_reschedule") } },
          onRequestExactAlarm = onRequestExactAlarm,
          onRequestIgnoreBatteryOpt = onRequestIgnoreBatteryOpt,
          onRequestNotifications = onRequestNotifications
        )
      }

      composable(Routes.DAYS) {
        DaysScreen(
          settings = settings,
          onBack = { nav.popBackStack() },
          onUpdateDayMode = { mode ->
            scope.launch {
              settingsRepo.setDayMode(mode)
              scheduler.rescheduleAll(reason = "dayMode")
            }
          },
          onUpdateCustomDays = { days ->
            scope.launch {
              settingsRepo.setCustomDays(days)
              scheduler.rescheduleAll(reason = "customDays")
            }
          }
        )
      }

      composable(Routes.SCHEDULE) {
        ScheduleScreen(
          settings = settings,
          onBack = { nav.popBackStack() },
          onSlotChanged = { idx, active, time ->
            scope.launch {
              settingsRepo.setSlotActive(idx, active)
              settingsRepo.setSlotTime(idx, time)
              scheduler.rescheduleAll(reason = "slots")
            }
          }
        )
      }

      composable(Routes.LOGIN) {
        LoginScreen(
          settings = settings,
          credsRepo = credsRepo,
          onBack = { nav.popBackStack() },
          onSaved = { scope.launch { logRepo.append("Credenciales actualizadas") } }
        )
      }

      composable(Routes.LOGS) {
        LogsScreen(
          logRepo = logRepo,
          onBack = { nav.popBackStack() }
        )
      }

      composable(Routes.ABOUT) {
        AboutScreen(onBack = { nav.popBackStack() })
      }
    }
  }
}

@Composable
private fun TopBar(nav: NavHostController) {
  val canBack = nav.previousBackStackEntry != null
  TopAppBar(
    title = { Text("Cámara AutoFichar") },
    navigationIcon = {
      if (canBack) {
        IconButton(onClick = { nav.popBackStack() }) {
          Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
        }
      }
    }
  )
}
