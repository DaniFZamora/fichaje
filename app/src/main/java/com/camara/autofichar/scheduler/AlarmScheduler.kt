package com.camara.autofichar.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.camara.autofichar.AppConstants
import com.camara.autofichar.data.SettingsRepository
import com.camara.autofichar.model.DayMode
import com.camara.autofichar.model.Settings
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AlarmScheduler(
  private val context: Context,
  private val settingsRepo: SettingsRepository
) {
  private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

  private fun pendingIntentForSlot(slotIndex: Int): PendingIntent {
    val i = Intent(context, AlarmReceiver::class.java).apply {
      action = AlarmReceiver.ACTION_RUN_SLOT
      putExtra(AlarmReceiver.EXTRA_SLOT_INDEX, slotIndex)
    }
    val req = 10_000 + slotIndex
    return PendingIntent.getBroadcast(
      context,
      req,
      i,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
  }

  suspend fun rescheduleAll(reason: String = "manual"): List<Long> {
    val s = settingsRepo.settingsFlow.first()

    if (!s.enabled) {
      cancelAll()
      settingsRepo.setNextRuns(List(AppConstants.SLOT_COUNT) { -1L })
      settingsRepo.setLastResult("OFF · alarmas canceladas ($reason)")
      return List(AppConstants.SLOT_COUNT) { -1L }
    }

    // Si custom sin días, no programar
    if (s.dayMode == DayMode.CUSTOM && !s.customDays.any { it }) {
      cancelAll()
      settingsRepo.setNextRuns(List(AppConstants.SLOT_COUNT) { -1L })
      settingsRepo.setLastResult("ON · modo personalizado sin días: no se programa nada")
      return List(AppConstants.SLOT_COUNT) { -1L }
    }

    val nextRuns = MutableList(AppConstants.SLOT_COUNT) { -1L }

    for (i in 0 until AppConstants.SLOT_COUNT) {
      cancelSlot(i)
      val slot = s.slots[i]
      if (!slot.active) continue

      val whenMs = computeNextRunForSlot(slot.timeHHmm, s)
      nextRuns[i] = whenMs
      scheduleSlot(i, whenMs)
    }

    settingsRepo.setNextRuns(nextRuns)
    settingsRepo.setLastResult("Reprogramado ($reason)")
    return nextRuns
  }

  fun cancelAll() {
    for (i in 0 until AppConstants.SLOT_COUNT) cancelSlot(i)
  }

  private fun cancelSlot(i: Int) {
    alarmManager.cancel(pendingIntentForSlot(i))
  }

  private fun scheduleSlot(i: Int, whenMs: Long) {
    val pi = pendingIntentForSlot(i)
    // En Android 12+ puede fallar si el usuario no ha permitido alarmas exactas.
    // Preferimos degradar a inexacta antes que crashear.
    try {
      alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
    } catch (se: SecurityException) {
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pi)
    }
  }

  private fun computeNextRunForSlot(timeHHmm: String, settings: Settings): Long {
    val (h, m) = parseHHmm(timeHHmm)
    val now = System.currentTimeMillis()

    // hoy
    run {
      val c = Calendar.getInstance()
      c.set(Calendar.HOUR_OF_DAY, h)
      c.set(Calendar.MINUTE, m)
      c.set(Calendar.SECOND, 0)
      c.set(Calendar.MILLISECOND, 0)
      val candidate = c.timeInMillis
      if (candidate > now && isAllowedDay(c, settings)) return candidate
    }

    // próximos 31 días
    for (i in 1..31) {
      val c = Calendar.getInstance()
      c.add(Calendar.DAY_OF_YEAR, i)
      c.set(Calendar.HOUR_OF_DAY, h)
      c.set(Calendar.MINUTE, m)
      c.set(Calendar.SECOND, 0)
      c.set(Calendar.MILLISECOND, 0)
      if (isAllowedDay(c, settings)) return c.timeInMillis
    }

    // fallback mañana
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, 1)
    c.set(Calendar.HOUR_OF_DAY, h)
    c.set(Calendar.MINUTE, m)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
  }

  private fun isAllowedDay(cal: Calendar, settings: Settings): Boolean {
    val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Dom..7=Sab
    val idx = dow - 1 // convert to 0=Dom..6=Sab

    return when (settings.dayMode) {
      DayMode.ALL -> true
      DayMode.WEEKDAYS -> idx in 1..5
      DayMode.CUSTOM -> settings.customDays.getOrNull(idx) == true
    }
  }

  private fun parseHHmm(s: String): Pair<Int, Int> {
    val parts = s.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0
    val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    return h to m
  }
}
