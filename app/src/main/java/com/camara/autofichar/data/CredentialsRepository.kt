package com.camara.autofichar.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class Credentials(val email: String, val password: String)

class CredentialsRepository(context: Context) {
  private val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

  private val prefs = EncryptedSharedPreferences.create(
    context,
    "creds",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
  )

  fun get(): Credentials {
    val email = prefs.getString("email", "") ?: ""
    val pass = prefs.getString("password", "") ?: ""
    return Credentials(email.trim(), pass)
  }

  fun set(email: String, password: String) {
    prefs.edit()
      .putString("email", email.trim())
      .putString("password", password)
      .apply()
  }

  fun clear() {
    prefs.edit().clear().apply()
  }
}
