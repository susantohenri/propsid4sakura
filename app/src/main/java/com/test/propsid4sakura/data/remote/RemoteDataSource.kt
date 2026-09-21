package com.test.propsid4sakura.data.remote

import android.content.Context
import com.test.propsid4sakura.AppConfig
import com.test.propsid4sakura.data.model.AdsConfig
import com.test.propsid4sakura.data.model.PropItem
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fetches remote JSON files independently.
 * Each fetch method is fully isolated — failure in one does NOT affect the other.
 *
 * [parseContentJson]: resilient parser supporting both flat array `[...]`
 *   and wrapped object `{"props_list": [...]}` formats.
 * [parseAdsConfigJson]: resilient parser supporting both current flat format
 *   and potential nested `admob_config` + `frequency` format.
 */
class RemoteDataSource(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val json: Json
) {

    // ---- Content / Props ----

    suspend fun fetchProps(): Result<List<PropItem>> = runCatching {
        val raw = fetch(AppConfig.CONTENT_URL)
        parseContentJson(raw)
    }

    fun parseContentJson(raw: String): List<PropItem> {
        val element = json.parseToJsonElement(raw)
        return when {
            element is JsonArray -> {
                // Current format: flat array at root
                json.decodeFromJsonElement(element)
            }
            element is JsonObject && element.containsKey("props_list") -> {
                // Legacy/future nested format
                json.decodeFromJsonElement(element["props_list"]!!)
            }
            else -> emptyList()
        }
    }

    // ---- Ads Config ----

    suspend fun fetchAdsConfig(): Result<AdsConfig> = runCatching {
        val raw = fetch(AppConfig.ADS_CONFIG_URL)
        parseAdsConfigJson(raw)
    }

    fun parseAdsConfigJson(raw: String): AdsConfig {
        val root = json.parseToJsonElement(raw).jsonObject
        return when {
            root.containsKey("admob_config") -> parseNestedAdsConfig(root)
            else -> parseFlatAdsConfig(root)
        }
    }

    /** Parse the current flat remote format */
    private fun parseFlatAdsConfig(root: JsonObject): AdsConfig {
        val isAdsEnabled = root["isAdsEnabled"]?.jsonPrimitive?.booleanOrNull ?: true
        return AdsConfig(
            appOpenAdUnitId = root["appOpenAdUnitId"]?.jsonPrimitive?.contentOrNull ?: "",
            bannerAdUnitId = root["bannerAdUnitId"]?.jsonPrimitive?.contentOrNull ?: "",
            interstitialAdUnitId = root["interstitialAdUnitId"]?.jsonPrimitive?.contentOrNull ?: "",
            rewardedAdUnitId = root["rewardedAdUnitId"]?.jsonPrimitive?.contentOrNull ?: "",
            nativeAdUnitId = root["nativeAdUnitId"]?.jsonPrimitive?.contentOrNull ?: "",
            isAdsEnabled = isAdsEnabled,
            // All per-format flags default to true; master isAdsEnabled controls them
            appOpenEnabled = true,
            bannerEnabled = true,
            interstitialEnabled = true,
            rewardedEnabled = true,
            nativeEnabled = true,
            // Frequency defaults (not in flat format — use AppConfig defaults)
            interstitialEveryN = AppConfig.DEFAULT_INTERSTITIAL_EVERY_N,
            interstitialMinIntervalSeconds = AppConfig.DEFAULT_INTERSTITIAL_MIN_INTERVAL_SECONDS,
            appOpenMinBgHours = AppConfig.DEFAULT_APP_OPEN_MIN_BG_HOURS,
            appOpenCooldownSeconds = AppConfig.DEFAULT_APP_OPEN_COOLDOWN_SECONDS,
            nativeEveryN = AppConfig.DEFAULT_NATIVE_EVERY_N
        )
    }

    /** Parse the nested format: { admob_config: {...}, frequency: {...} } */
    private fun parseNestedAdsConfig(root: JsonObject): AdsConfig {
        val admobConfig = root["admob_config"]?.jsonObject ?: JsonObject(emptyMap())
        val frequency = root["frequency"]?.jsonObject ?: JsonObject(emptyMap())

        fun adUnit(key: String) = admobConfig[key]?.jsonObject?.get("ad_unit_id")?.jsonPrimitive?.contentOrNull ?: ""
        fun isEnabled(key: String) = admobConfig[key]?.jsonObject?.get("is_enabled")?.jsonPrimitive?.booleanOrNull ?: true
        fun freqInt(key: String, default: Int) = frequency[key]?.jsonPrimitive?.intOrNull ?: default
        fun freqLong(key: String, default: Long) = frequency[key]?.jsonPrimitive?.longOrNull ?: default

        return AdsConfig(
            appOpenAdUnitId = adUnit("app_open"),
            bannerAdUnitId = adUnit("collapsed_banner"),
            interstitialAdUnitId = adUnit("interstitial"),
            rewardedAdUnitId = adUnit("rewarded"),
            nativeAdUnitId = adUnit("native"),
            isAdsEnabled = true,
            appOpenEnabled = isEnabled("app_open"),
            bannerEnabled = isEnabled("collapsed_banner"),
            interstitialEnabled = isEnabled("interstitial"),
            rewardedEnabled = isEnabled("rewarded"),
            nativeEnabled = isEnabled("native"),
            interstitialEveryN = freqInt("interstitial_every_n_detail_opens", AppConfig.DEFAULT_INTERSTITIAL_EVERY_N),
            interstitialMinIntervalSeconds = freqLong("interstitial_min_interval_seconds", AppConfig.DEFAULT_INTERSTITIAL_MIN_INTERVAL_SECONDS),
            appOpenMinBgHours = freqLong("app_open_min_background_hours", AppConfig.DEFAULT_APP_OPEN_MIN_BG_HOURS),
            appOpenCooldownSeconds = freqLong("app_open_cooldown_after_any_ad_seconds", AppConfig.DEFAULT_APP_OPEN_COOLDOWN_SECONDS),
            nativeEveryN = freqInt("native_every_n_list_items", AppConfig.DEFAULT_NATIVE_EVERY_N)
        )
    }

    // ---- Load asset fallback ----

    fun loadAssetAdsConfig(): AdsConfig = runCatching {
        val raw = context.assets.open("ads_config.json").bufferedReader().readText()
        parseAdsConfigJson(raw)
    }.getOrDefault(AdsConfig())

    fun loadAssetProps(): List<PropItem> = runCatching {
        val raw = context.assets.open("content.json").bufferedReader().readText()
        parseContentJson(raw)
    }.getOrDefault(emptyList())

    // ---- HTTP helper ----

    private fun fetch(url: String): String {
        val request = Request.Builder().url(url).build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code} for $url")
            response.body?.string() ?: throw Exception("Empty body for $url")
        }
    }
}
