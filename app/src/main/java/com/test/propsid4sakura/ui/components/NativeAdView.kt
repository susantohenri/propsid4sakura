package com.test.propsid4sakura.ui.components

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.test.propsid4sakura.R

/**
 * Composable wrapper for a Native Ad.
 * Inflates the native_ad_layout XML and binds all NativeAd assets.
 */
@Composable
fun NativeAdItem(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            val adView = LayoutInflater.from(context)
                .inflate(R.layout.native_ad_layout, null) as NativeAdView
            bindNativeAd(adView, nativeAd)
            adView
        },
        update = { adView -> bindNativeAd(adView, nativeAd) },
        modifier = modifier.fillMaxWidth()
    )
}

private fun bindNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline
    (adView.bodyView as? TextView)?.text = nativeAd.body
    (adView.callToActionView as? Button)?.text = nativeAd.callToAction
    nativeAd.icon?.let {
        (adView.iconView as? ImageView)?.setImageDrawable(it.drawable)
    }
    (adView.starRatingView as? RatingBar)?.rating = nativeAd.starRating?.toFloat() ?: 0f
    adView.setNativeAd(nativeAd)
}
