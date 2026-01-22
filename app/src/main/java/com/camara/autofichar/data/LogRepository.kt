package com.camara.autofichar.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogRepository(private val context: Context) {
  private val file: File get() = File(context.filesDir, "runlog.txt")
  private val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

  suspend fun append(message: String) {
    withContext(Dispatchers.IO) {
      val line = "${df.format(Date())}  $message"
      file.appendText(line + "\n")
      trimIfNeeded(maxLines = 800)
    }
  }

  suspend fun readTail(maxLines: Int = 400): List<String> {
    return withContext(Dispatchers.IO) {
      if (!file.exists()) return@withContext emptyList<String>()
      val lines = file.readLines()
      if (lines.size <= maxLines) lines else lines.takeLast(maxLines)
    }
  }

  suspend fun clear() {
    withContext(Dispatchers.IO) {
      if (file.exists()) file.writeText("")
    }
  }

  private fun trimIfNeeded(maxLines: Int) {
    if (!file.exists()) return
    val lines = file.readLines()
    if (lines.size <= maxLines) return
    val keep = lines.takeLast(maxLines)
    file.writeText(keep.joinToString("\n") + "\n")
  }
}
