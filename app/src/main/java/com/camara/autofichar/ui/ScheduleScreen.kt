package com.camara.autofichar.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.camara.autofichar.model.Settings
import java.util.Calendar

@Composable
fun ScheduleScreen(
  settings: Settings,
  onBack: () -> Unit,
  onSlotChanged: (index: Int, active: Boolean, timeHHmm: String) -> Unit
) {
  val ctx = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("Horarios (4 slots)", style = MaterialTheme.typography.titleLarge)

    settings.slots.forEachIndexed { idx, slot ->
      ElevatedCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${slot.label} ${idx + 1}", fontWeight = FontWeight.SemiBold)
            Switch(
              checked = slot.active,
              onCheckedChange = { onSlotChanged(idx, it, slot.timeHHmm) }
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Hora: ${slot.timeHHmm}")
            OutlinedButton(
              enabled = slot.active,
              onClick = {
                val cal = Calendar.getInstance()
                val parts = slot.timeHHmm.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
                val m = parts.getOrNull(1)?.toIntOrNull() ?: cal.get(Calendar.MINUTE)
                TimePickerDialog(
                  ctx,
                  { _, hour, minute ->
                    val hh = hour.toString().padStart(2, '0')
                    val mm = minute.toString().padStart(2, '0')
                    onSlotChanged(idx, slot.active, "$hh:$mm")
                  },
                  h,
                  m,
                  true
                ).show()
              }
            ) {
              Text("Cambiar")
            }
          }

          Text(
            "Se reprograma automáticamente al cambiar algo.",
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onBack) { Text("Volver") }
  }
}
