package com.camara.autofichar.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.core.Preferences
import com.camara.autofichar.AppConstants
import com.camara.autofichar.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val context: Context) {

  private object Keys {
    val ENABLED = booleanPreferencesKey("enabled")
    val DAY_MODE = stringPreferencesKey("day_mode")
    val CUSTOM_DAYS = stringPreferencesKey("custom_days") // 7 chars 0/1, 0=Dom
    val THEME = stringPreferencesKey("theme")

    fun slotActive(i: Int) = booleanPreferencesKey("slot_${i}_active")
    fun slotTime(i: Int) = stringPreferencesKey("slot_${i}_time")
    fun nextRun(i: Int) = longPreferencesKey("next_${i}")

    val LAST_RESULT = stringPreferencesKey("last_result")
    val LAST_RESULT_AT = longPreferencesKey("last_result_at")
  }

  val settingsFlow: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
    readSettings(prefs)
  }

  suspend fun setEnabled(enabled: Boolean) {
    context.settingsDataStore.edit { it[Keys.ENABLED] = enabled }
  }

  suspend fun setDayMode(mode: DayMode) {
    context.settingsDataStore.edit { it[Keys.DAY_MODE] = mode.id }
  }

  suspend fun setCustomDays(days: BooleanArray) {
    val safe = normalizeDays(days)
    val encoded = safe.joinToString(separator = "") { if (it) "1" else "0" }
    context.settingsDataStore.edit { it[Keys.CUSTOM_DAYS] = encoded }
  }

  suspend fun setSlotActive(index: Int, active: Boolean) {
    if (index !in 0 until AppConstants.SLOT_COUNT) return
    context.settingsDataStore.edit { it[Keys.slotActive(index)] = active }
  }

  suspend fun setSlotTime(index: Int, timeHHmm: String) {
    if (index !in 0 until AppConstants.SLOT_COUNT) return
    val safe = normalizeTime(timeHHmm) ?: AppConstants.DEFAULT_TIMES[index]
    context.settingsDataStore.edit { it[Keys.slotTime(index)] = safe }
  }

  suspend fun setTheme(theme: ThemeMode) {
    context.settingsDataStore.edit { it[Keys.THEME] = theme.id }
  }

  suspend fun setNextRuns(nextRuns: List<Long>) {
    val safe = (0 until AppConstants.SLOT_COUNT).map { i -> nextRuns.getOrNull(i) ?: -1L }
    context.settingsDataStore.edit { e ->
      safe.forEachIndexed { i, v -> e[Keys.nextRun(i)] = v }
    }
  }

  suspend fun setLastResult(text: String) {
    context.settingsDataStore.edit {
      it[Keys.LAST_RESULT] = text
      it[Keys.LAST_RESULT_AT] = System.currentTimeMillis()
    }
  }

  private fun readSettings(prefs: Preferences): Settings {
    val enabled = prefs[Keys.ENABLED] ?: false
    val dayMode = DayMode.fromId(prefs[Keys.DAY_MODE])

    val customDays = parseDays(prefs[Keys.CUSTOM_DAYS])

    val slots = (0 until AppConstants.SLOT_COUNT).map { i ->
      val active = prefs[Keys.slotActive(i)] ?: true
      val time = normalizeTime(prefs[Keys.slotTime(i)] ?: AppConstants.DEFAULT_TIMES[i]) ?: AppConstants.DEFAULT_TIMES[i]
      Slot(active = active, label = AppConstants.DEFAULT_LABELS[i], timeHHmm = time)
    }

    val theme = ThemeMode.fromId(prefs[Keys.THEME])

    val nextRuns = (0 until AppConstants.SLOT_COUNT).map { i -> prefs[Keys.nextRun(i)] ?: -1L }

    val lastResult = prefs[Keys.LAST_RESULT] ?: "—"
    val lastResultAt = prefs[Keys.LAST_RESULT_AT] ?: 0L

    return Settings(
      enabled = enabled,
      dayMode = dayMode,
      customDays = customDays,
      slots = slots,
      theme = theme,
      nextRuns = nextRuns,
      lastResult = lastResult,
      lastResultAt = lastResultAt
    )
  }

  private fun parseDays(encoded: String?): BooleanArray {
    val def = booleanArrayOf(false, true, true, true, true, true, false)
    if (encoded.isNullOrBlank() || encoded.length < 7) return def
    val arr = BooleanArray(7)
    for (i in 0..6) {
      arr[i] = encoded.getOrNull(i) == '1'
    }
    return arr
  }

  private fun normalizeDays(days: BooleanArray?): BooleanArray {
    val def = booleanArrayOf(false, true, true, true, true, true, false)
    if (days == null || days.size != 7) return def
    return BooleanArray(7) { i -> days[i] }
  }

  private fun normalizeTime(t: String?): String? {
    val s = (t ?: "").trim()
    return if (Regex("^\\d{2}:\\d{2}$").matches(s)) s else null
  }
}
