package com.ameermuawiya.apksigner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ameermuawiya.apksigner.R
import com.ameermuawiya.apksigner.data.model.AppSignDetails
import com.ameermuawiya.apksigner.ui.components.CardGroupPosition
import com.ameermuawiya.apksigner.ui.components.ErrorLoggerDialog
import com.ameermuawiya.apksigner.ui.components.ExpressiveLoadingIndicator
import com.ameermuawiya.apksigner.ui.components.KeystoreManagementSheet
import com.ameermuawiya.apksigner.ui.components.KeystorePasswordDialog
import com.ameermuawiya.apksigner.ui.components.getGroupedCardShape
import com.ameermuawiya.apksigner.ui.theme.ApkSignerTheme
import com.ameermuawiya.apksigner.ui.viewmodel.MainViewModel
import com.ameermuawiya.apksigner.ui.viewmodel.SigningUiState

/**
 * Screen presenting APK signing configuration, signature schemes, key selection, and progress logs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SignPackageScreen(
    details: AppSignDetails,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = viewModel.settingsManager

    val isAnalyzing by viewModel.isAppDetailsLoading.collectAsState()
    val signingState by viewModel.signingState.collectAsState()
    val liveLogs by viewModel.liveLogs.collectAsState()

    val isSigning = signingState is SigningUiState.Signing
    val lastErrorLog = (signingState as? SigningUiState.Error)?.errorLog
    val signedSuccessPath = (signingState as? SigningUiState.Success)?.outputPath

    val v1 by settingsManager.v1Scheme.collectAsState()
    val v2 by settingsManager.v2Scheme.collectAsState()
    val v3 by settingsManager.v3Scheme.collectAsState()
    val v4 by settingsManager.v4Scheme.collectAsState()

    val customKeyPath by settingsManager.customKeyPath.collectAsState()
    val customKeysList by settingsManager.customKeysList.collectAsState()

    val showKeystoreDialog by viewModel.showKeystoreDialog.collectAsState()

    var showKeySheet by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    val logListState = rememberLazyListState()

    LaunchedEffect(liveLogs.size) {
        if (liveLogs.isNotEmpty()) {
            logListState.animateScrollToItem(liveLogs.size - 1)
        }
    }

    BackHandler {
        if (!isSigning) {
            onBack()
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = { Text(text = stringResource(R.string.sign_package_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSigning) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.sign_package_screen_back_desc)
                        )
                    }
                },
                actions = {
                    if (lastErrorLog != null) {
                        IconButton(onClick = { showErrorDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = stringResource(R.string.sign_package_screen_view_error_log),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        if (isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                ExpressiveLoadingIndicator(
                    size = 56.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val hasBottomCard = isSigning || signedSuccessPath != null || lastErrorLog != null

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val appInfoShape = getGroupedCardShape(CardGroupPosition.FIRST)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(appInfoShape),
                        shape = appInfoShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (details.icon != null) {
                                    Image(
                                        bitmap = details.icon.toBitmap(56, 56).asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp)
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Android,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = details.appName,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = details.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "v${details.versionName} • ${details.fileSizeFormatted}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.sign_package_screen_detected_sig),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val detectedList = mutableListOf<String>()
                                    if (details.existingV1) detectedList.add(stringResource(R.string.sign_package_screen_v1))
                                    if (details.existingV2) detectedList.add(stringResource(R.string.sign_package_screen_v2))
                                    if (details.existingV3) detectedList.add(stringResource(R.string.sign_package_screen_v3))
                                    if (details.existingV4) detectedList.add(stringResource(R.string.sign_package_screen_v4))

                                    if (detectedList.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.height(22.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.sign_package_screen_unsigned),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    } else {
                                        detectedList.forEach { scheme ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.height(22.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                ) {
                                                    Text(
                                                        text = scheme,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val configPos = if (hasBottomCard) CardGroupPosition.MIDDLE else CardGroupPosition.LAST
                    val configShape = getGroupedCardShape(configPos)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(configShape),
                        shape = configShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.sign_package_screen_config),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            val selectorShape = RoundedCornerShape(14.dp)
                            Surface(
                                shape = selectorShape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(selectorShape)
                                    .clickable(enabled = !isSigning) { showKeySheet = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Key,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    val activeRecord = if (!customKeyPath.isNullOrBlank()) customKeysList.find { it.path == customKeyPath } else null
                                    val hasCustomTitle = !activeRecord?.name.isNullOrBlank()

                                    Column(modifier = Modifier.weight(1f)) {
                                        if (activeRecord != null) {
                                            if (hasCustomTitle) {
                                                Text(
                                                    text = activeRecord.name,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Alias: ${activeRecord.alias}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Text(
                                                    text = activeRecord.alias,
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Format: ${activeRecord.format}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = stringResource(R.string.sign_package_screen_key_default),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Alias: androiddebugkey",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Text(
                                        text = stringResource(R.string.sign_package_screen_change),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = stringResource(R.string.sign_package_screen_schemes),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = v1,
                                    onClick = { if (!isSigning) settingsManager.setV1Scheme(!v1) },
                                    label = { Text(text = stringResource(R.string.sign_package_screen_v1)) },
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                                FilterChip(
                                    selected = v2,
                                    onClick = { if (!isSigning) settingsManager.setV2Scheme(!v2) },
                                    label = { Text(text = stringResource(R.string.sign_package_screen_v2)) },
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                                FilterChip(
                                    selected = v3,
                                    onClick = { if (!isSigning) settingsManager.setV3Scheme(!v3) },
                                    label = { Text(text = stringResource(R.string.sign_package_screen_v3)) },
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                                FilterChip(
                                    selected = v4,
                                    onClick = { if (!isSigning) settingsManager.setV4Scheme(!v4) },
                                    label = { Text(text = stringResource(R.string.sign_package_screen_v4)) },
                                    border = null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    if (isSigning) {
                        val progressShape = getGroupedCardShape(CardGroupPosition.LAST)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(progressShape),
                            shape = progressShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ExpressiveLoadingIndicator(
                                    size = 28.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                val currentStepMsg = (signingState as? SigningUiState.Signing)?.message ?: "Signing in progress..."
                                Text(
                                    text = currentStepMsg,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (signedSuccessPath != null) {
                        val successShape = getGroupedCardShape(CardGroupPosition.LAST)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(successShape),
                            shape = successShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = stringResource(R.string.sign_package_screen_success_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = stringResource(R.string.sign_package_screen_success_msg, signedSuccessPath),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.installSignedApk(signedSuccessPath)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = stringResource(R.string.sign_package_screen_install))
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.shareSignedFile(signedSuccessPath)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = stringResource(R.string.sign_package_screen_share))
                                    }
                                }
                            }
                        }
                    }

                    if (lastErrorLog != null) {
                        val errorShape = getGroupedCardShape(CardGroupPosition.LAST)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(errorShape),
                            shape = errorShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(44.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = stringResource(R.string.sign_package_screen_error_title),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = stringResource(R.string.sign_package_screen_error_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = { showErrorDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        )
                                    ) {
                                        Icon(imageVector = Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = stringResource(R.string.sign_package_screen_view_error_log))
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.startSigning() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = stringResource(R.string.sign_package_screen_retry))
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isSigning && signedSuccessPath == null && lastErrorLog == null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.startSigning() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sign_package_screen_sign_now),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                if (liveLogs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.sign_package_screen_live_logs),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                IconButton(
                                    onClick = {
                                        val fullLogs = liveLogs.joinToString("\n")
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Execution Logs", fullLogs)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, context.getString(R.string.sign_package_screen_logs_copied), Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = stringResource(R.string.sign_package_screen_copy_logs),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                LazyColumn(state = logListState) {
                                    items(liveLogs) { log ->
                                        val logColor = when {
                                            log.startsWith("[SUCCESS]") || log.startsWith("[OK]") || log.contains("Verified", ignoreCase = true) -> Color(0xFF2E7D32)
                                            log.startsWith("[ERROR]") || log.startsWith("[EXCEPTION]") -> Color(0xFFD32F2F)
                                            log.startsWith("[STEP]") || log.startsWith("[CONFIG]") || log.startsWith("[LOCATION]") || log.startsWith("[OUTPUT]") -> MaterialTheme.colorScheme.primary
                                            log.startsWith("[WARN]") -> Color(0xFFED6C02)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }

                                        Text(
                                            text = log,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                lineHeight = 16.sp
                                            ),
                                            color = logColor,
                                            modifier = Modifier.padding(vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showKeySheet) {
            val sheetState = rememberModalBottomSheetState()
            KeystoreManagementSheet(
                activeKeyPath = customKeyPath,
                customKeysList = customKeysList,
                sheetState = sheetState,
                onSelectKey = { record ->
                    viewModel.selectCustomKeyRecord(record)
                },
                onDeleteKey = { id ->
                    viewModel.deleteCustomKeyRecord(id)
                },
                onAddKeyClick = {
                    showKeySheet = false
                    viewModel.openKeyPickerDialog()
                },
                onDismiss = { showKeySheet = false }
            )
        }

        if (showKeystoreDialog) {
            KeystorePasswordDialog(
                onInspectKeystore = { file, pw, callback ->
                    viewModel.inspectKeystore(file, pw, callback)
                },
                onSaveKey = { path, alias, format, details, password, customTitle ->
                    viewModel.saveCustomKeystore(path, alias, format, details, password, customTitle)
                },
                onDismiss = { viewModel.dismissKeystoreDialog() }
            )
        }

        if (showErrorDialog && lastErrorLog != null) {
            ErrorLoggerDialog(
                errorLog = lastErrorLog,
                onDismiss = { showErrorDialog = false }
            )
        }
    }
}

/**
 * Preview composable for SignPackageScreen in light and dark themes.
 */
@Preview(showBackground = true)
@Composable
fun SignPackageScreenPreview() {
    ApkSignerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Text("SignPackageScreen Preview", modifier = Modifier.padding(16.dp))
        }
    }
}
