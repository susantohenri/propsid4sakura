package com.test.propsid4sakura.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.propsid4sakura.data.local.PropEntity
import com.test.propsid4sakura.data.repository.PropsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favorites: List<PropEntity> = emptyList(),
    val isLoading: Boolean = true
)

class FavoritesViewModel(
    private val propsRepository: PropsRepository
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = propsRepository.observeFavorites()
        .map { FavoritesUiState(favorites = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FavoritesUiState()
        )

    fun toggleFavorite(prop: PropEntity) {
        viewModelScope.launch {
            propsRepository.setFavorite(prop.propId, !prop.isFavorite)
        }
    }
}
