package com.test.propsid4sakura.ui.components

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.*
import com.test.propsid4sakura.data.model.AdsConfig

/**
 * Collapsible banner ad at the bottom of the screen.
 * Uses adaptive banner size for best fill.
 * Only rendered when [adsConfig.bannerEffectivelyEnabled] is true.
 */
@Composable
fun BannerAdView(
    adsConfig: AdsConfig,
    modifier: Modifier = Modifier
) {
    if (!adsConfig.bannerEffectivelyEnabled) return
    val adUnitId = adsConfig.bannerAdUnitId.ifBlank { return }

    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, getScreenWidth(ctx)))
                setAdUnitId(adUnitId)
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = modifier
    )
}

private fun getScreenWidth(context: Context): Int {
    val displayMetrics = context.resources.displayMetrics
    return (displayMetrics.widthPixels / displayMetrics.density).toInt()
}
