package com.camara.autofichar.ui

import android.app.AlarmManager
import android.content.Context
import android.os.PowerManager
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.camara.autofichar.model.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
  settings: Settings,
  onGoDays: () -> Unit,
  onGoSchedule: () -> Unit,
  onGoLogin: () -> Unit,
  onGoLogs: () -> Unit,
  onGoAbout: () -> Unit,
  onToggleEnabled: (Boolean) -> Unit,
  onRunNow: () -> Unit,
  onReschedule: () -> Unit,
  onRequestExactAlarm: () -> Unit,
  onRequestIgnoreBatteryOpt: () -> Unit,
  onRequestNotifications: () -> Unit
) {
  val ctx = LocalContext.current
  val alarmOk = canScheduleExactAlarms(ctx)
  val batteryOk = isIgnoringBatteryOptimizations(ctx)

  val df = rememberDf()
  val next = settings.nextRuns.filter { it > 0 }.minOrNull()
  val nextText = if (next != null) df.format(Date(next)) else "—"

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text("Estado", style = MaterialTheme.typography.titleMedium)
        Text(
          if (settings.enabled) "ON" else "OFF",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold
        )
      }
      Switch(
        checked = settings.enabled,
        onCheckedChange = { onToggleEnabled(it) }
      )
    }

    ElevatedCard {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Próxima ejecución", style = MaterialTheme.typography.titleSmall)
        Text(nextText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        Text("Último resultado", style = MaterialTheme.typography.titleSmall)
        val lastAt = if (settings.lastResultAt > 0) df.format(Date(settings.lastResultAt)) else "—"
        Text("${settings.lastResult} · $lastAt")
      }
    }

    if (!alarmOk) {
      AssistChip(
        onClick = onRequestExactAlarm,
        label = { Text("Falta permiso de alarmas exactas · Toca para activarlo") }
      )
    }

    if (!batteryOk) {
      AssistChip(
        onClick = onRequestIgnoreBatteryOpt,
        label = { Text("Ahorro de batería activo · Recomendado: permitir ejecución sin restricciones") }
      )
    }

    if (Build.VERSION.SDK_INT >= 33) {
      AssistChip(
        onClick = onRequestNotifications,
        label = { Text("Permiso notificaciones (Android 13+) · Toca para pedirlo") }
      )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Button(onClick = onRunNow, enabled = true) { Text("Ejecutar ahora") }
      OutlinedButton(onClick = onReschedule) { Text("Reprogramar") }
    }

    Divider()

    ListItem(
      headlineContent = { Text("Días") },
      supportingContent = { Text("L–V / todos / personalizado") },
      modifier = Modifier.fillMaxWidth(),
      trailingContent = { TextButton(onClick = onGoDays) { Text("Abrir") } }
    )

    ListItem(
      headlineContent = { Text("Horarios") },
      supportingContent = { Text("4 slots configurables") },
      modifier = Modifier.fillMaxWidth(),
      trailingContent = { TextButton(onClick = onGoSchedule) { Text("Abrir") } }
    )

    ListItem(
      headlineContent = { Text("Login") },
      supportingContent = { Text("Credenciales cifradas en el dispositivo") },
      modifier = Modifier.fillMaxWidth(),
      trailingContent = { TextButton(onClick = onGoLogin) { Text("Abrir") } }
    )

    ListItem(
      headlineContent = { Text("Logs") },
      supportingContent = { Text("Qué ha pasado en cada ejecución") },
      modifier = Modifier.fillMaxWidth(),
      trailingContent = { TextButton(onClick = onGoLogs) { Text("Abrir") } }
    )

    ListItem(
      headlineContent = { Text("Acerca de") },
      supportingContent = { Text("Limitaciones reales y recomendaciones") },
      modifier = Modifier.fillMaxWidth(),
      trailingContent = { TextButton(onClick = onGoAbout) { Text("Abrir") } }
    )
  }
}

@Composable
private fun rememberDf(): SimpleDateFormat =
  androidx.compose.runtime.remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

private fun canScheduleExactAlarms(ctx: Context): Boolean {
  if (Build.VERSION.SDK_INT < 31) return true
  val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  return am.canScheduleExactAlarms()
}

private fun isIgnoringBatteryOptimizations(ctx: Context): Boolean {
  // Si no hay Doze/optimización (API < 23), damos OK.
  if (Build.VERSION.SDK_INT < 23) return true
  val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
  return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

