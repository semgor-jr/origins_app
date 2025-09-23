package com.origin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore("session")

object SessionKeys {
	val TOKEN = stringPreferencesKey("token")
	val NAME = stringPreferencesKey("name")
	val FONT_SIZE = floatPreferencesKey("font_size")
}

class SessionStore(private val context: Context) {
	val token: Flow<String?> = context.dataStore.data.map { it[SessionKeys.TOKEN] }
	val name: Flow<String?> = context.dataStore.data.map { it[SessionKeys.NAME] }
	val fontSize: Flow<Float> = context.dataStore.data.map { it[SessionKeys.FONT_SIZE] ?: 1.0f }

	suspend fun save(token: String?, name: String?) {
		context.dataStore.edit { prefs ->
			token?.let { prefs[SessionKeys.TOKEN] = it } ?: prefs.remove(SessionKeys.TOKEN)
			name?.let { prefs[SessionKeys.NAME] = it } ?: prefs.remove(SessionKeys.NAME)
		}
	}

	suspend fun clearSession() {
		context.dataStore.edit { prefs ->
			prefs.remove(SessionKeys.TOKEN)
			prefs.remove(SessionKeys.NAME)
		}
	}
	
	suspend fun setFontSize(size: Float) {
		context.dataStore.edit { prefs ->
			prefs[SessionKeys.FONT_SIZE] = size
		}
	}
	
	fun getFontSize(): Float {
		return runBlocking { 
			context.dataStore.data.map { it[SessionKeys.FONT_SIZE] ?: 1.0f }.first()
		}
	}
}
