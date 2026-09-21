package com.test.propsid4sakura.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.test.propsid4sakura.AppConfig
import com.test.propsid4sakura.BuildConfig
import com.test.propsid4sakura.R
import com.test.propsid4sakura.ads.ConsentManager
import com.test.propsid4sakura.data.local.AppPreferences
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    consentManager: ConsentManager,
    onBack: () -> Unit
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            // ---- PREFERENCES SECTION ----
            SettingsSectionTitle(stringResource(R.string.pref_section))

            // Language
            SettingsItem(
                title = stringResource(R.string.language_label),
                content = {
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = uiState.language == AppPreferences.LANG_ID || uiState.language == AppPreferences.LANG_SYSTEM,
                            onClick = {
                                viewModel.setLanguage(AppPreferences.LANG_ID)
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("id"))
                            },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text(stringResource(R.string.language_id)) }

                        SegmentedButton(
                            selected = uiState.language == AppPreferences.LANG_EN,
                            onClick = {
                                viewModel.setLanguage(AppPreferences.LANG_EN)
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                            },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text(stringResource(R.string.language_en)) }
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Theme
            SettingsItem(
                title = stringResource(R.string.theme_label),
                content = {
                    SingleChoiceSegmentedButtonRow {
                        listOf(
                            AppPreferences.THEME_SYSTEM to stringResource(R.string.theme_system),
                            AppPreferences.THEME_LIGHT to stringResource(R.string.theme_light),
                            AppPreferences.THEME_DARK to stringResource(R.string.theme_dark)
                        ).forEachIndexed { index, pair ->
                            val value: String = pair.first
                            val label: String = pair.second
                            SegmentedButton(
                                selected = uiState.theme == value,
                                onClick = { viewModel.setTheme(value) },
                                shape = SegmentedButtonDefaults.itemShape(index, 3)
                            ) { Text(label) }
                        }
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // ---- LEGAL & INFO SECTION ----
            SettingsSectionTitle(stringResource(R.string.legal_section))

            // About
            ListItem(
                headlineContent = { Text(stringResource(R.string.about_label)) },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable { showAboutDialog = true }
            )

            // Privacy Policy
            ListItem(
                headlineContent = { Text(stringResource(R.string.privacy_policy_label)) },
                leadingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PRIVACY_POLICY_URL))
                    context.startActivity(intent)
                }
            )

            // Privacy Settings (GDPR UMP — only shown when required)
            if (consentManager.isPrivacyOptionsRequired) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.privacy_settings_label)) },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.clickable {
                        activity?.let { consentManager.showPrivacyOptionsForm(it) }
                    }
                )
            }
        }
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(stringResource(R.string.about_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.disclaimer_full),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
