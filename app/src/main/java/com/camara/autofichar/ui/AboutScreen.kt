package com.camara.autofichar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(onBack: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("Acerca de", style = MaterialTheme.typography.titleLarge)

    Text(
      "Esto automatiza una web. Si la web cambia, tu app tiene que adaptarse. " +
        "No hay magia.",
      style = MaterialTheme.typography.bodyMedium
    )

    ElevatedCard {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Qué hace", style = MaterialTheme.typography.titleMedium)
        Text("• Programa 4 horarios")
        Text("• Abre la URL del portal")
        Text("• Si detecta login: rellena y entra")
        Text("• Espera 10–20s")
        Text("• Pulsa el botón de fichar")
      }
    }

    ElevatedCard {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Limitaciones reales", style = MaterialTheme.typography.titleMedium)
        Text("• Si aparece captcha o 2FA, no se puede automatizar")
        Text("• Algunos modos de ahorro de batería pueden retrasar alarmas")
        Text("• Si el botón cambia (id/texto), hay que ajustar el selector")
      }
    }

    Text(
      "Recomendación: en Ajustes del móvil, pon la app sin restricciones de batería.",
      style = MaterialTheme.typography.bodyMedium
    )

    OutlinedButton(onClick = onBack) { Text("Volver") }
  }
}
