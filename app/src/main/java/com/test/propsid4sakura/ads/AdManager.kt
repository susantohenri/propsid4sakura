package com.test.propsid4sakura.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.test.propsid4sakura.data.local.AppPreferences
import com.test.propsid4sakura.data.model.AdsConfig
import com.test.propsid4sakura.data.repository.AdsConfigRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

private const val TAG = "AdManager"

/**
 * Central AdManager that coordinates all ad types.
 *
 * Rules enforced here:
 * - MobileAds.initialize is called only once, after consent.
 * - All ads respect isAdsEnabled + per-format enabled flags.
 * - Central [lastAnyAdMs] prevents two ads from showing back-to-back.
 * - App Open Ad: only on foregrounding, not cold start, respects cooldown.
 * - Interstitial: every N detail opens, min interval respected.
 * - Rewarded: only via user action; auto-preload after watched/closed.
 */
class AdManager(
    private val context: Context,
    private val adsConfigRepository: AdsConfigRepository,
    private val prefs: AppPreferences,
    private val scope: CoroutineScope
) {
    private var initialized = false
    private var isColdStart = true

    // In-memory ad objects
    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null

    // Tracking
    private var isShowingAd = false

    private val adsConfig: AdsConfig
        get() = adsConfigRepository.adsConfig.value

    // ---- Initialization ----

    /** Call once after UMP consent is granted */
    fun initialize(activity: Activity, onReady: () -> Unit) {
        if (initialized) { onReady(); return }
        MobileAds.initialize(activity) {
            initialized = true
            Log.d(TAG, "MobileAds initialized")
            preloadAds()
            onReady()
        }
    }

    private fun preloadAds() {
        if (!adsConfig.isAdsEnabled) return
        if (adsConfig.rewardedEffectivelyEnabled) loadRewarded()
        if (adsConfig.interstitialEffectivelyEnabled) loadInterstitial()
        if (adsConfig.appOpenEffectivelyEnabled) loadAppOpen()
    }

    // ---- Rewarded ----

    private fun loadRewarded() {
        if (!adsConfig.rewardedEffectivelyEnabled) return
        val adUnitId = adsConfig.rewardedAdUnitId.ifBlank { return }
        val request = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, request, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                Log.d(TAG, "Rewarded ad loaded")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                rewardedAd = null
                Log.w(TAG, "Rewarded failed: ${error.message}")
                // Retry after 30 s
                scope.launch { delay(30_000); loadRewarded() }
            }
        })
    }

    /**
     * Show the rewarded ad. Calls [onUserEarnedReward] only in the reward callback.
     * Calls [onAdClosed] when done (whether reward earned or not).
     * Never opens the prop for free if the ad fails to show.
     */
    fun showRewarded(
        activity: Activity,
        onUserEarnedReward: () -> Unit,
        onAdClosed: () -> Unit,
        onAdFailedToShow: () -> Unit
    ) {
        if (!adsConfig.rewardedEffectivelyEnabled) {
            // Rewarded disabled via config -> treat as free unlock
            onUserEarnedReward()
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            onAdFailedToShow()
            loadRewarded()
            return
        }
        isShowingAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                isShowingAd = false
                recordAnyAd()
                onAdClosed()
                loadRewarded()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                isShowingAd = false
                onAdFailedToShow()
                loadRewarded()
            }
        }
        ad.show(activity) { _ -> onUserEarnedReward() }
    }

    val isRewardedReady: Boolean get() = rewardedAd != null

    // ---- Interstitial ----

    private fun loadInterstitial() {
        if (!adsConfig.interstitialEffectivelyEnabled) return
        val adUnitId = adsConfig.interstitialAdUnitId.ifBlank { return }
        InterstitialAd.load(context, adUnitId, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.w(TAG, "Interstitial failed: ${error.message}")
                }
            })
    }

    /**
     * Called every time user opens a detail screen.
     * Shows interstitial when counter reaches N and interval is met.
     * Must NOT show right after a rewarded ad (checks last any-ad time).
     */
    fun onDetailOpened(activity: Activity) {
        if (!adsConfig.interstitialEffectivelyEnabled) return
        if (isShowingAd) return

        scope.launch {
            prefs.incrementDetailOpenCounter()
            val counter = prefs.detailOpenCounterFlow.first()
            val lastInterstitialMs = prefs.lastInterstitialMsFlow.first()
            val lastAnyAdMs = prefs.lastAnyAdMsFlow.first()
            val now = System.currentTimeMillis()

            val intervalOk = (now - lastInterstitialMs) >= adsConfig.interstitialMinIntervalSeconds * 1000
            val cooldownOk = (now - lastAnyAdMs) >= adsConfig.appOpenCooldownSeconds * 1000
            val counterOk = counter >= adsConfig.interstitialEveryN

            if (counterOk && intervalOk && cooldownOk) {
                prefs.resetDetailOpenCounter()
                withContext(Dispatchers.Main) { showInterstitial(activity) }
            }
        }
    }

    private fun showInterstitial(activity: Activity) {
        val ad = interstitialAd ?: return
        isShowingAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                isShowingAd = false
                recordInterstitial()
                loadInterstitial()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                isShowingAd = false
                loadInterstitial()
            }
        }
        ad.show(activity)
    }

    // ---- App Open ----

    private fun loadAppOpen() {
        if (!adsConfig.appOpenEffectivelyEnabled) return
        val adUnitId = adsConfig.appOpenAdUnitId.ifBlank { return }
        val request = AdRequest.Builder().build()
        AppOpenAd.load(context, adUnitId, request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    Log.d(TAG, "App open ad loaded")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    Log.w(TAG, "App open failed: ${error.message}")
                }
            })
    }

    /**
     * Called when the app comes to foreground.
     * Shows app open ad only if:
     * - Not cold start
     * - Time in background >= [AdsConfig.appOpenMinBgHours] hours
     * - Cooldown since any ad >= [AdsConfig.appOpenCooldownSeconds]
     * - No other ad is currently showing
     */
    fun onAppForegrounded(activity: Activity) {
        if (!adsConfig.appOpenEffectivelyEnabled) return
        if (isShowingAd) return
        if (isColdStart) { isColdStart = false; return }

        scope.launch {
            val lastBgMs = prefs.lastBgTimeMsFlow.first()
            val lastAnyAdMs = prefs.lastAnyAdMsFlow.first()
            val now = System.currentTimeMillis()

            val bgHours = (now - lastBgMs) / (1000 * 3600)
            val cooldownOk = (now - lastAnyAdMs) >= adsConfig.appOpenCooldownSeconds * 1000
            val bgOk = bgHours >= adsConfig.appOpenMinBgHours

            if (bgOk && cooldownOk) {
                withContext(Dispatchers.Main) { showAppOpen(activity) }
            }
        }
    }

    fun onAppBackgrounded() {
        scope.launch { prefs.recordBackgroundTime() }
    }

    private fun showAppOpen(activity: Activity) {
        val ad = appOpenAd ?: run { loadAppOpen(); return }
        isShowingAd = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                recordAnyAd()
                loadAppOpen()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                appOpenAd = null
                isShowingAd = false
                loadAppOpen()
            }
        }
        ad.show(activity)
    }

    // ---- Helpers ----

    private fun recordInterstitial() {
        scope.launch { prefs.recordInterstitialShown() }
    }

    private fun recordAnyAd() {
        scope.launch { prefs.recordAnyAdShown() }
    }

    /** Whether adManager is ready to show ads (initialized and ads enabled) */
    val isReady: Boolean get() = initialized && adsConfig.isAdsEnabled
}
