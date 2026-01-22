package com.camara.autofichar.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.camara.autofichar.automation.AutomationService

class AlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != ACTION_RUN_SLOT) return

    val idx = intent.getIntExtra(EXTRA_SLOT_INDEX, -1)
    if (idx !in 0..3) return

    val sIntent = Intent(context, AutomationService::class.java).apply {
      putExtra(AutomationService.EXTRA_SLOT_INDEX, idx)
      putExtra(AutomationService.EXTRA_TRIGGER, "alarm")
    }

    context.startForegroundService(sIntent)
  }

  companion object {
    const val ACTION_RUN_SLOT = "com.camara.autofichar.RUN_SLOT"
    const val EXTRA_SLOT_INDEX = "slotIndex"
  }
}
