package com.test.propsid4sakura

/**
 * Central configuration constants for the application.
 * Update URLs here to point to different remote JSON sources.
 */
object AppConfig {

    /** Remote URL for ads configuration JSON */
    const val ADS_CONFIG_URL =
        "https://raw.githubusercontent.com/susantohenri/admob-remote-configs/refs/heads/main/propsid4sakura/ads_config.json"

    /** Remote URL for props list JSON */
    const val CONTENT_URL =
        "https://raw.githubusercontent.com/susantohenri/admob-remote-configs/refs/heads/main/propsid4sakura/content.json"

    /** Base URL for prop images — same folder as content.json */
    const val IMAGE_BASE_URL =
        "https://raw.githubusercontent.com/susantohenri/admob-remote-configs/refs/heads/main/propsid4sakura/"

    /** Returns the full image URL for a given prop_id */
    fun imageUrl(propId: String): String = "$IMAGE_BASE_URL$propId.webp"

    // ---- Ad Frequency Defaults (used when remote config does not provide them) ----
    const val DEFAULT_INTERSTITIAL_EVERY_N = 3
    const val DEFAULT_INTERSTITIAL_MIN_INTERVAL_SECONDS = 60L
    const val DEFAULT_APP_OPEN_MIN_BG_HOURS = 4L
    const val DEFAULT_APP_OPEN_COOLDOWN_SECONDS = 60L
    const val DEFAULT_NATIVE_EVERY_N = 8

    // ---- Network ----
    const val NETWORK_TIMEOUT_SECONDS = 15L

    /** Play Store link used in share text */
    const val PLAY_STORE_URL =
        "https://play.google.com/store/apps/details?id=com.test.propsid4sakura"

    /** Privacy Policy URL */
    const val PRIVACY_POLICY_URL =
        "https://tokiocv.blogspot.com/2026/07/privacy-policy.html"
}
