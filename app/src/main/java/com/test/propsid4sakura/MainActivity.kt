package com.test.propsid4sakura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.test.propsid4sakura.ads.AdManager
import com.test.propsid4sakura.ads.ConsentManager
import com.test.propsid4sakura.data.local.AppPreferences
import com.test.propsid4sakura.data.local.AppPreferences.Companion.LANG_EN
import com.test.propsid4sakura.data.local.AppPreferences.Companion.LANG_ID
import com.test.propsid4sakura.data.local.AppPreferences.Companion.LANG_SYSTEM
import com.test.propsid4sakura.data.local.AppPreferences.Companion.THEME_DARK
import com.test.propsid4sakura.data.local.AppPreferences.Companion.THEME_LIGHT
import com.test.propsid4sakura.data.repository.AdsConfigRepository
import com.test.propsid4sakura.data.repository.PropsRepository
import com.test.propsid4sakura.ui.navigation.AppNavigation
import com.test.propsid4sakura.ui.theme.PropsIDTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val prefs: AppPreferences by inject()
    private val consentManager: ConsentManager by inject()
    private val adManager: AdManager by inject()
    private val propsRepository: PropsRepository by inject()
    private val adsConfigRepository: AdsConfigRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // SplashScreen API — install before super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load ads config fallback immediately (never wait for network)
        adsConfigRepository.loadFallback()

        // Apply saved language preference immediately
        lifecycleScope.launch {
            val lang = prefs.languageFlow.first()
            applyLanguage(lang)
        }

        // Start background sync (props + ads config)
        lifecycleScope.launch {
            launch { propsRepository.syncFromRemote() }
            launch { adsConfigRepository.syncFromRemote() }
        }

        // UMP Consent flow — init ads only after consent
        consentManager.requestAndShow(this) { canRequestAds ->
            if (canRequestAds) {
                adManager.initialize(this) {
                    // Ads system ready
                }
            }
        }

        setContent {
            val theme by prefs.themeFlow.collectAsState(initial = AppPreferences.THEME_SYSTEM)

            val darkTheme = when (theme) {
                THEME_DARK -> true
                THEME_LIGHT -> false
                else -> null // null = follow system
            }

            PropsIDTheme(darkTheme = darkTheme) {
                AppNavigation(
                    adManager = adManager,
                    consentManager = consentManager
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adManager.onAppForegrounded(this)
    }

    override fun onPause() {
        super.onPause()
        adManager.onAppBackgrounded()
    }

    private fun applyLanguage(lang: String) {
        val localeList = when (lang) {
            LANG_ID -> LocaleListCompat.forLanguageTags("id")
            LANG_EN -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList() // system default
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
