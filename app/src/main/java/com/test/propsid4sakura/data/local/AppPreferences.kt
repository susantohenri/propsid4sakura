package com.test.propsid4sakura.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

/**
 * DataStore-backed preferences for:
 * - Theme (SYSTEM / LIGHT / DARK)
 * - Language (SYSTEM / id / en)
 * - Ad timestamps & counters (for cooldown & frequency logic)
 * - First launch disclaimer shown flag
 */
class AppPreferences(private val context: Context) {

    companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_LAST_INTERSTITIAL_MS = longPreferencesKey("last_interstitial_ms")
        val KEY_LAST_ANY_AD_MS = longPreferencesKey("last_any_ad_ms")
        val KEY_DETAIL_OPEN_COUNTER = intPreferencesKey("detail_open_counter")
        val KEY_DISCLAIMER_SHOWN = booleanPreferencesKey("disclaimer_shown")
        val KEY_LAST_BG_TIME_MS = longPreferencesKey("last_bg_time_ms")

        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"

        const val LANG_SYSTEM = "SYSTEM"
        const val LANG_ID = "id"
        const val LANG_EN = "en"
    }

    val themeFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_THEME] ?: THEME_SYSTEM }

    val languageFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LANGUAGE] ?: LANG_SYSTEM }

    val disclaimerShownFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DISCLAIMER_SHOWN] ?: false }

    val detailOpenCounterFlow: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_DETAIL_OPEN_COUNTER] ?: 0 }

    val lastInterstitialMsFlow: Flow<Long> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LAST_INTERSTITIAL_MS] ?: 0L }

    val lastAnyAdMsFlow: Flow<Long> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LAST_ANY_AD_MS] ?: 0L }

    val lastBgTimeMsFlow: Flow<Long> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LAST_BG_TIME_MS] ?: 0L }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun setDisclaimerShown() {
        context.dataStore.edit { it[KEY_DISCLAIMER_SHOWN] = true }
    }

    suspend fun incrementDetailOpenCounter() {
        context.dataStore.edit {
            it[KEY_DETAIL_OPEN_COUNTER] = (it[KEY_DETAIL_OPEN_COUNTER] ?: 0) + 1
        }
    }

    suspend fun resetDetailOpenCounter() {
        context.dataStore.edit { it[KEY_DETAIL_OPEN_COUNTER] = 0 }
    }

    suspend fun recordInterstitialShown() {
        val now = System.currentTimeMillis()
        context.dataStore.edit {
            it[KEY_LAST_INTERSTITIAL_MS] = now
            it[KEY_LAST_ANY_AD_MS] = now
        }
    }

    suspend fun recordAnyAdShown() {
        context.dataStore.edit { it[KEY_LAST_ANY_AD_MS] = System.currentTimeMillis() }
    }

    suspend fun recordBackgroundTime() {
        context.dataStore.edit { it[KEY_LAST_BG_TIME_MS] = System.currentTimeMillis() }
    }
}
