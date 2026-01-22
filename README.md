# Cámara AutoFichar (Android)

App Android nativa (Kotlin + Jetpack Compose) que replica tu extensión:

- 4 horarios configurables
- Días: L–V / todos / personalizado
- Credenciales guardadas **cifradas** (Android Keystore)
- Alarmas exactas (AlarmManager) + ForegroundService para fiabilidad
- Web automation con WebView + JS (login si aparece + click en `#ficharButton`)
- Logs dentro de la app

## Cómo ejecutar

1. Abre el proyecto en **Android Studio**
2. Deja que sincronice Gradle
3. Ejecuta en un dispositivo o emulador Android (recomendado dispositivo real)

## Ajustes importantes del móvil

- Activa **alarmas exactas** si tu Android te lo pide
- En batería/energía, pon la app en **sin restricciones** (si no, algunas marcas retrasan alarmas)
- En Android 13+ acepta notificaciones (se usa para el servicio en primer plano)

## Cómo funciona

En cada ejecución:

1. Carga `https://camarazamora.crecepersonas.es/`
2. Si detecta el formulario `/login`, rellena email+password y hace submit
3. Espera 10–20s (aleatorio)
4. Intenta click en `#ficharButton` (y hace 1 retry)

Si la web cambia (HTML / IDs), hay que ajustar `WebViewAutomator.kt`.
