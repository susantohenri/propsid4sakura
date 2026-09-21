package com.test.propsid4sakura.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.*
import com.test.propsid4sakura.BuildConfig

/**
 * Manages Google UMP (User Messaging Platform) consent flow.
 *
 * Call [requestAndShow] on every app start.
 * Ads initialization must happen only after [canRequestAds] returns true.
 */
class ConsentManager(private val context: Context) {

    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    /** Whether ads can be requested (consent given or not required) */
    val canRequestAds: Boolean
        get() = consentInformation.canRequestAds()

    /** Whether privacy options form is required (GDPR) */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Request consent info update and show form if required.
     * [onComplete] is called after consent is determined, passing true if ads can be requested.
     */
    fun requestAndShow(
        activity: Activity,
        onComplete: (canRequestAds: Boolean) -> Unit
    ) {
        val params = ConsentRequestParameters.Builder().apply {
            if (BuildConfig.DEBUG) {
                val debugBuilder = ConsentDebugSettings.Builder(context)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                if (BuildConfig.CONSENT_DEBUG_DEVICE_HASH.isNotBlank() && BuildConfig.CONSENT_DEBUG_DEVICE_HASH != "TEST-DEVICE-HASH") {
                    debugBuilder.addTestDeviceHashedId(BuildConfig.CONSENT_DEBUG_DEVICE_HASH)
                }
                setConsentDebugSettings(debugBuilder.build())
            }
        }.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Success — load and show form if required
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    // formError is non-null only if form failed to show
                    // In either case, proceed and check canRequestAds
                    onComplete(consentInformation.canRequestAds())
                }
            },
            { requestError ->
                // Consent info request failed — proceed without consent form
                // (e.g., offline); canRequestAds might still be true from cached info
                onComplete(consentInformation.canRequestAds())
            }
        )
    }

    /**
     * Show privacy options form (for the "Privacy Settings" menu item in Settings).
     * Only call when [isPrivacyOptionsRequired] is true.
     */
    fun showPrivacyOptionsForm(activity: Activity, onDismiss: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onDismiss() }
    }
}
