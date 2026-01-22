package com.camara.autofichar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.camara.autofichar.data.LogRepository
import kotlinx.coroutines.launch

@Composable
fun LogsScreen(
  logRepo: LogRepository,
  onBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  var lines by remember { mutableStateOf<List<String>>(emptyList()) }
  var loading by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    loading = true
    lines = logRepo.readTail()
    loading = false
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("Logs", style = MaterialTheme.typography.titleLarge)

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(onClick = {
        scope.launch {
          loading = true
          lines = logRepo.readTail()
          loading = false
        }
      }) { Text("Actualizar") }

      OutlinedButton(onClick = {
        scope.launch {
          logRepo.clear()
          lines = emptyList()
        }
      }) { Text("Borrar") }
    }

    if (loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    SelectionContainer {
      Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
        ) {
          if (lines.isEmpty()) {
            Text("—", fontFamily = FontFamily.Monospace)
          } else {
            lines.forEach { Text(it, fontFamily = FontFamily.Monospace) }
          }
        }
      }
    }

    OutlinedButton(onClick = onBack) { Text("Volver") }
  }
}
