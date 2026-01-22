package com.camara.autofichar.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action ?: return
    if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return

    val req = OneTimeWorkRequestBuilder<RescheduleWorker>()
      .setInputData(workDataOf("reason" to "boot"))
      .build()

    WorkManager.getInstance(context).enqueue(req)
  }
}
