package com.test.propsid4sakura.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.test.propsid4sakura.data.local.AppPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: String = AppPreferences.THEME_SYSTEM,
    val language: String = AppPreferences.LANG_SYSTEM,
    val disclaimerShown: Boolean = false
)

class SettingsViewModel(
    private val prefs: AppPreferences
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.themeFlow,
        prefs.languageFlow,
        prefs.disclaimerShownFlow
    ) { theme, language, disclaimerShown ->
        SettingsUiState(theme = theme, language = language, disclaimerShown = disclaimerShown)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setTheme(theme: String) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch { prefs.setLanguage(language) }
    }

    fun setDisclaimerShown() {
        viewModelScope.launch { prefs.setDisclaimerShown() }
    }
}
