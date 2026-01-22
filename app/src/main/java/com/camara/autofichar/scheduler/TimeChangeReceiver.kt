package com.camara.autofichar.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class TimeChangeReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    // Nota: ACTION_TIME_CHANGED == "android.intent.action.TIME_SET"
    if (action != Intent.ACTION_TIME_CHANGED && action != Intent.ACTION_TIMEZONE_CHANGED) return

    val req = OneTimeWorkRequestBuilder<RescheduleWorker>()
      .setInputData(workDataOf("reason" to "time_change"))
      .build()

    WorkManager.getInstance(context).enqueue(req)
  }
}
