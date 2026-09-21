package com.test.propsid4sakura.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.test.propsid4sakura.AppConfig
import com.test.propsid4sakura.R
import com.test.propsid4sakura.ads.AdManager
import com.test.propsid4sakura.ui.components.ShimmerBox
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    propId: String,
    adManager: AdManager,
    onBack: () -> Unit
) {
    val viewModel: DetailViewModel = koinViewModel(parameters = { parametersOf(propId) })
    val uiState by viewModel.uiState.collectAsState()
    val adsConfig by viewModel.adsConfig.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when message is set
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarShown()
        }
    }

    val prop = uiState.prop

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(prop?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (prop != null) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (prop.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (prop.isFavorite) stringResource(R.string.remove_favorite) else stringResource(R.string.add_favorite),
                                tint = if (prop.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        if (uiState.isLoading || prop == null) {
            Box(
                Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) CircularProgressIndicator()
                else Text(stringResource(R.string.error_loading))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large prop image
            AsyncImage(
                model = AppConfig.imageUrl(prop.propId),
                contentDescription = prop.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.ic_image_placeholder),
                error = painterResource(R.drawable.ic_image_error)
            )

            Spacer(Modifier.height(16.dp))

            // Category chip
            AssistChip(
                onClick = {},
                label = { Text(prop.category) }
            )

            Spacer(Modifier.height(16.dp))

            // ID section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.prop_id_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))

                    if (prop.isUnlocked || !adsConfig.rewardedEffectivelyEnabled) {
                        // UNLOCKED — show clear ID
                        Text(
                            text = prop.propId,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                        Spacer(Modifier.height(12.dp))

                        // Action buttons — spaced away from each other, not near ads
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Copy button
                            Button(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("prop_id", prop.propId))
                                viewModel.showSnackbar(context.getString(R.string.id_copied))
                            }) {
                                Text(stringResource(R.string.copy_id))
                            }

                            // Share button
                            OutlinedButton(onClick = {
                                shareProp(context, prop.propId, prop.title, prop.category)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.share))
                            }
                        }
                    } else {
                        // LOCKED — blur placeholder
                        Text(
                            text = stringResource(R.string.blurred_id_placeholder),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            modifier = Modifier.blur(6.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(12.dp))

                        when (uiState.adState) {
                            is DetailAdState.Loading -> {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Text(
                                    stringResource(R.string.ad_loading),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            is DetailAdState.NotAvailable -> {
                                Text(
                                    stringResource(R.string.ad_not_available),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(8.dp))
                                // Retry button — not adjacent to any ad placement
                                Button(onClick = {
                                    if (!adManager.isRewardedReady) {
                                        viewModel.setAdState(DetailAdState.NotAvailable)
                                    } else {
                                        activity?.let { triggerRewardedAd(it, adManager, viewModel) }
                                    }
                                }) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                            else -> {
                                // Idle — show Watch Ad button (user must press it voluntarily)
                                Button(onClick = {
                                    if (activity != null) {
                                        triggerRewardedAd(activity, adManager, viewModel)
                                    }
                                }) {
                                    Text(stringResource(R.string.watch_ad_to_unlock))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun triggerRewardedAd(
    activity: android.app.Activity,
    adManager: AdManager,
    viewModel: DetailViewModel
) {
    viewModel.setAdState(DetailAdState.Loading)
    adManager.showRewarded(
        activity = activity,
        onUserEarnedReward = { viewModel.onRewardEarned() },
        onAdClosed = { viewModel.setAdState(DetailAdState.Idle) },
        onAdFailedToShow = { viewModel.setAdState(DetailAdState.NotAvailable) }
    )
}

private fun shareProp(context: Context, propId: String, title: String, category: String) {
    val shareText = context.getString(R.string.share_text, title, category, propId)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    // Attach cached image if available
    val cachedImage = File(context.cacheDir, "prop_images/$propId.webp")
    if (cachedImage.exists()) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cachedImage)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_title)))
}
