package com.test.propsid4sakura.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.test.propsid4sakura.data.local.PropEntity
import com.test.propsid4sakura.data.model.AdsConfig
import com.test.propsid4sakura.data.repository.AdsConfigRepository
import com.test.propsid4sakura.data.repository.PropsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val allProps: List<PropEntity> = emptyList(),
    val filteredItems: List<Any> = emptyList(), // PropEntity or NativeAd
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val propsRepository: PropsRepository,
    private val adsConfigRepository: AdsConfigRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("")
    private val _searchQuery = MutableStateFlow("")
    private val _nativeAds = MutableStateFlow<List<NativeAd>>(emptyList())

    val adsConfig: StateFlow<AdsConfig> = adsConfigRepository.adsConfig

    val uiState: StateFlow<HomeUiState> = combine(
        propsRepository.observeAllProps(),
        _selectedCategory,
        _searchQuery,
        adsConfigRepository.adsConfig,
        _nativeAds
    ) { props, category, query, config, nativeAds ->
        val filtered = props
            .filter { prop ->
                (category.isEmpty() || prop.category == category) &&
                (query.isEmpty() || prop.title.contains(query, ignoreCase = true))
            }

        val categories = props.map { it.category }.distinct().sorted()

        // Interleave native ads every N items
        val items: List<Any> = if (config.nativeEffectivelyEnabled && nativeAds.isNotEmpty()) {
            buildList {
                val n = config.nativeEveryN
                var adIndex = 0
                filtered.forEachIndexed { index, prop ->
                    add(prop)
                    if ((index + 1) % n == 0 && adIndex < nativeAds.size) {
                        add(nativeAds[adIndex++])
                    }
                }
            }
        } else {
            filtered
        }

        HomeUiState(
            allProps = props,
            filteredItems = items,
            categories = categories,
            selectedCategory = category,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavorite(prop: PropEntity) {
        viewModelScope.launch {
            propsRepository.setFavorite(prop.propId, !prop.isFavorite)
        }
    }
}
