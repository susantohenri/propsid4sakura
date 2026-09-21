package com.test.propsid4sakura.data.model

import com.test.propsid4sakura.AppConfig

/**
 * Parsed ads configuration, supporting both the current flat remote format and
 * the potential nested format (admob_config + frequency).
 *
 * All fields have safe defaults so the app never crashes on missing keys.
 */
data class AdsConfig(
    // Ad Unit IDs
    val appOpenAdUnitId: String = "",
    val bannerAdUnitId: String = "",
    val interstitialAdUnitId: String = "",
    val rewardedAdUnitId: String = "",
    val nativeAdUnitId: String = "",

    // Master switch — if false, all ads are disabled, all props unlocked free
    val isAdsEnabled: Boolean = true,

    // Per-format enable flags (for nested format; default true when absent)
    val appOpenEnabled: Boolean = true,
    val bannerEnabled: Boolean = true,
    val interstitialEnabled: Boolean = true,
    val rewardedEnabled: Boolean = true,
    val nativeEnabled: Boolean = true,

    // Frequency / Cooldown settings
    val interstitialEveryN: Int = AppConfig.DEFAULT_INTERSTITIAL_EVERY_N,
    val interstitialMinIntervalSeconds: Long = AppConfig.DEFAULT_INTERSTITIAL_MIN_INTERVAL_SECONDS,
    val appOpenMinBgHours: Long = AppConfig.DEFAULT_APP_OPEN_MIN_BG_HOURS,
    val appOpenCooldownSeconds: Long = AppConfig.DEFAULT_APP_OPEN_COOLDOWN_SECONDS,
    val nativeEveryN: Int = AppConfig.DEFAULT_NATIVE_EVERY_N
) {
    /**
     * Whether rewarded ads are effectively active.
     * If false, all props are unlocked for free.
     */
    val rewardedEffectivelyEnabled: Boolean
        get() = isAdsEnabled && rewardedEnabled

    val appOpenEffectivelyEnabled: Boolean
        get() = isAdsEnabled && appOpenEnabled

    val interstitialEffectivelyEnabled: Boolean
        get() = isAdsEnabled && interstitialEnabled

    val bannerEffectivelyEnabled: Boolean
        get() = isAdsEnabled && bannerEnabled

    val nativeEffectivelyEnabled: Boolean
        get() = isAdsEnabled && nativeEnabled
}
