package com.camara.autofichar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.camara.autofichar.data.CredentialsRepository
import com.camara.autofichar.model.Settings

@Composable
fun LoginScreen(
  settings: Settings,
  credsRepo: CredentialsRepository,
  onBack: () -> Unit,
  onSaved: () -> Unit
) {
  val existing = remember { credsRepo.get() }
  var email by remember { mutableStateOf(existing.email) }
  var pass by remember { mutableStateOf(existing.password) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Text("Login", style = MaterialTheme.typography.titleLarge)

    Text(
      "Solo se usa si la web muestra el formulario de acceso. " +
        "Si ya estás logueado, no toca nada.",
      style = MaterialTheme.typography.bodyMedium
    )

    OutlinedTextField(
      value = email,
      onValueChange = { email = it },
      label = { Text("Email") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
      value = pass,
      onValueChange = { pass = it },
      label = { Text("Contraseña") },
      visualTransformation = PasswordVisualTransformation(),
      singleLine = true,
      modifier = Modifier.fillMaxWidth()
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Button(onClick = {
        credsRepo.set(email, pass)
        onSaved()
      }) { Text("Guardar") }

      OutlinedButton(onClick = {
        credsRepo.clear()
        email = ""
        pass = ""
        onSaved()
      }) { Text("Borrar") }
    }

    Divider()

    Text(
      "Seguridad: se guarda cifrado con Android Keystore en el dispositivo.",
      style = MaterialTheme.typography.bodySmall
    )

    OutlinedButton(onClick = onBack) { Text("Volver") }
  }
}
