package com.test.propsid4sakura.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.propsid4sakura.data.local.PropEntity
import com.test.propsid4sakura.data.model.AdsConfig
import com.test.propsid4sakura.data.repository.AdsConfigRepository
import com.test.propsid4sakura.data.repository.PropsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class DetailAdState {
    object Idle : DetailAdState()
    object Loading : DetailAdState()
    object NotAvailable : DetailAdState()
}

data class DetailUiState(
    val prop: PropEntity? = null,
    val isLoading: Boolean = true,
    val adState: DetailAdState = DetailAdState.Idle,
    val snackbarMessage: String? = null
)

class DetailViewModel(
    private val propId: String,
    private val propsRepository: PropsRepository,
    private val adsConfigRepository: AdsConfigRepository,
    // adManager is accessed from the screen directly for Activity reference
    private val dummy: Any? = null
) : ViewModel() {

    val adsConfig: StateFlow<AdsConfig> = adsConfigRepository.adsConfig

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prop = propsRepository.getPropById(propId)
            _uiState.update { it.copy(prop = prop, isLoading = false) }
        }
    }

    fun setAdState(state: DetailAdState) {
        _uiState.update { it.copy(adState = state) }
    }

    /** Called from onUserEarnedReward callback — unlocks the prop permanently */
    fun onRewardEarned() {
        viewModelScope.launch {
            propsRepository.unlockProp(propId)
            val updated = propsRepository.getPropById(propId)
            _uiState.update { it.copy(prop = updated, adState = DetailAdState.Idle) }
        }
    }

    fun toggleFavorite() {
        val prop = _uiState.value.prop ?: return
        viewModelScope.launch {
            propsRepository.setFavorite(propId, !prop.isFavorite)
            val updated = propsRepository.getPropById(propId)
            _uiState.update { it.copy(prop = updated) }
        }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun snackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
