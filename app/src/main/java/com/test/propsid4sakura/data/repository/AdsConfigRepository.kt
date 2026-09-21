package com.test.propsid4sakura.data.repository

import com.test.propsid4sakura.data.model.AdsConfig
import com.test.propsid4sakura.data.remote.RemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository for AdMob configuration.
 *
 * Strategy:
 * 1. Initialize immediately with asset fallback so ads config is always available.
 * 2. Fetch remote config in background and update the StateFlow if successful.
 */
class AdsConfigRepository(
    private val remoteDataSource: RemoteDataSource
) {
    private val _adsConfig = MutableStateFlow(AdsConfig())
    val adsConfig: StateFlow<AdsConfig> = _adsConfig.asStateFlow()

    /** Load asset fallback immediately (call on app start, before any network) */
    fun loadFallback() {
        _adsConfig.value = remoteDataSource.loadAssetAdsConfig()
    }

    /**
     * Fetch remote config in background and update if successful.
     * Failures are silently ignored — fallback remains active.
     */
    suspend fun syncFromRemote() = withContext(Dispatchers.IO) {
        remoteDataSource.fetchAdsConfig()
            .onSuccess { config -> _adsConfig.value = config }
        // On failure: existing config continues — no action needed
    }
}
