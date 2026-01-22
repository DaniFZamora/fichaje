package com.camara.autofichar.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.camara.autofichar.data.SettingsRepository

class RescheduleWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    val settings = SettingsRepository(applicationContext)
    val scheduler = AlarmScheduler(applicationContext, settings)
    scheduler.rescheduleAll(reason = inputData.getString("reason") ?: "worker")
    return Result.success()
  }
}
