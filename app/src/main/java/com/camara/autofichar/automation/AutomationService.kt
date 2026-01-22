package com.camara.autofichar.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.camara.autofichar.AppConstants
import com.camara.autofichar.R
import com.camara.autofichar.data.CredentialsRepository
import com.camara.autofichar.data.LogRepository
import com.camara.autofichar.data.SettingsRepository
import com.camara.autofichar.scheduler.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class AutomationService : Service() {
  private val job = SupervisorJob()
  private val scope = CoroutineScope(Dispatchers.Main.immediate + job)

  private lateinit var settingsRepo: SettingsRepository
  private lateinit var credsRepo: CredentialsRepository
  private lateinit var logRepo: LogRepository

  private var automator: WebViewAutomator? = null

  override fun onCreate() {
    super.onCreate()
    settingsRepo = SettingsRepository(applicationContext)
    credsRepo = CredentialsRepository(applicationContext)
    logRepo = LogRepository(applicationContext)
    ensureChannel()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val slotIndex = intent?.getIntExtra(EXTRA_SLOT_INDEX, -1) ?: -1
    val trigger = intent?.getStringExtra(EXTRA_TRIGGER) ?: "unknown"

    // Evita ejecuciones concurrentes (dos alarmas seguidas, doble tap, etc.)
    val now = System.currentTimeMillis()
    if (!RUNNING.compareAndSet(false, true)) {
      scope.launch {
        logRepo.append("Bloqueado: ya hay una ejecución en curso")
      }
      stopSelf()
      return START_NOT_STICKY
    }
    lastStartAt = now

    startForeground(NOTIF_ID, buildNotif("Ejecutando fichaje…"))

    scope.launch {
      runOnce(slotIndex, trigger)
    }

    return START_NOT_STICKY
  }

  private suspend fun runOnce(slotIndex: Int, trigger: String) {
    // 0) Reprograma al empezar (para no perder el siguiente slot si esta ejecución se alarga)
    withContext(Dispatchers.Default) {
      val scheduler = AlarmScheduler(applicationContext, settingsRepo)
      scheduler.rescheduleAll(reason = "pre_run")
    }

    // 1) Si no hay red, corta rápido (no te quedes 95s colgado)
    if (!hasNetwork()) {
      logRepo.append("Sin red: abort")
      settingsRepo.setLastResult("ERROR · no_network")
      finishAndStop()
      return
    }

    val creds = credsRepo.get()

    logRepo.append("Inicio · trigger=$trigger · slot=$slotIndex")

    val delayMs = WebViewAutomator.randomDelayMs(10_000, 20_000)

    automator = WebViewAutomator(
      context = applicationContext,
      url = AppConstants.FIXED_URL,
      email = creds.email,
      password = creds.password,
      delayMs = delayMs,
      logger = { msg -> scope.launch { logRepo.append(msg) } },
      onDone = { ok, detail ->
        scope.launch {
          val resultText = if (ok) "OK · $detail" else "ERROR · $detail"
          logRepo.append("Fin · $resultText")
          settingsRepo.setLastResult(resultText)

          finishAndStop()
        }
      }
    )

    automator?.start()
  }

  override fun onDestroy() {
    super.onDestroy()
    automator?.destroy()
    automator = null
    job.cancel()
    RUNNING.set(false)
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun ensureChannel() {
    if (Build.VERSION.SDK_INT < 26) return
    val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    val ch = NotificationChannel(
      CHANNEL_ID,
      "AutoFichar",
      NotificationManager.IMPORTANCE_LOW
    )
    ch.description = "Ejecución de fichaje automático"
    mgr.createNotificationChannel(ch)
  }

  private fun buildNotif(text: String): Notification {
    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_notify_sync)
      .setContentTitle("Cámara AutoFichar")
      .setContentText(text)
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .build()
  }

  companion object {
    const val EXTRA_SLOT_INDEX = "extra_slot_index"
    const val EXTRA_TRIGGER = "extra_trigger"

    private const val CHANNEL_ID = "autofichar"
    private const val NOTIF_ID = 1001

    private val RUNNING = AtomicBoolean(false)
    @Volatile private var lastStartAt: Long = 0L
  }

  private fun hasNetwork(): Boolean {
    val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    val nw = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(nw) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }

  private fun finishAndStop() {
    RUNNING.set(false)
    stopSelf()
  }
}
