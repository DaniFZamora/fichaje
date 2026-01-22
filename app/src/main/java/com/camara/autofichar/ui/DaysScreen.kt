package com.camara.autofichar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.camara.autofichar.model.DayMode
import com.camara.autofichar.model.Settings

private val DAY_LABELS = listOf("D", "L", "M", "X", "J", "V", "S")
private val DAY_TITLES = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun DaysScreen(
  settings: Settings,
  onBack: () -> Unit,
  onUpdateDayMode: (DayMode) -> Unit,
  onUpdateCustomDays: (BooleanArray) -> Unit
) {
  var localMode by remember(settings.dayMode) { mutableStateOf(settings.dayMode) }
  var days by remember(settings.customDays) { mutableStateOf(settings.customDays.copyOf()) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("Días de ejecución", style = MaterialTheme.typography.titleLarge)

    SegmentedButtonRow {
      SegmentedButton(
        selected = localMode == DayMode.WEEKDAYS,
        onClick = {
          localMode = DayMode.WEEKDAYS
          onUpdateDayMode(DayMode.WEEKDAYS)
        },
        label = { Text("L–V") }
      )
      SegmentedButton(
        selected = localMode == DayMode.ALL,
        onClick = {
          localMode = DayMode.ALL
          onUpdateDayMode(DayMode.ALL)
        },
        label = { Text("Todos") }
      )
      SegmentedButton(
        selected = localMode == DayMode.CUSTOM,
        onClick = {
          localMode = DayMode.CUSTOM
          onUpdateDayMode(DayMode.CUSTOM)
        },
        label = { Text("Personal") }
      )
    }

    if (localMode == DayMode.CUSTOM) {
      Text("Selecciona días", style = MaterialTheme.typography.titleMedium)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0..6) {
          FilterChip(
            selected = days[i],
            onClick = {
              days[i] = !days[i]
              onUpdateCustomDays(days.copyOf())
            },
            label = { Text(DAY_LABELS[i]) },
            leadingIcon = null
          )
        }
      }

      Spacer(Modifier.height(6.dp))
      Text(
        "Consejo: si dejas 0 días marcados, no se programará nada.",
        style = MaterialTheme.typography.bodySmall
      )
    }

    Divider()

    Text("Resumen", style = MaterialTheme.typography.titleMedium)
    val summary = when (localMode) {
      DayMode.WEEKDAYS -> "Se ejecuta de lunes a viernes."
      DayMode.ALL -> "Se ejecuta todos los días."
      DayMode.CUSTOM -> {
        val selected = (0..6).filter { days[it] }.joinToString { DAY_TITLES[it] }
        if (selected.isBlank()) "No hay días seleccionados." else "Se ejecuta: $selected"
      }
    }
    Text(summary)

    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = onBack) { Text("Volver") }
  }
}
