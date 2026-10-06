package com.ameermuawiya.apksigner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ameermuawiya.apksigner.R
import com.ameermuawiya.apksigner.ui.components.CardGroupPosition
import com.ameermuawiya.apksigner.ui.components.ExpressiveLoadingIndicator
import com.ameermuawiya.apksigner.ui.components.ExpressivePullToRefreshBox
import com.ameermuawiya.apksigner.ui.components.getGroupedCardShape
import com.ameermuawiya.apksigner.ui.theme.ApkSignerTheme
import com.ameermuawiya.apksigner.ui.viewmodel.HistoryUiItem
import com.ameermuawiya.apksigner.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * History screen displaying previously signed packages with dynamic app icons, key alias chips, and file operations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val historyUiItems by viewModel.historyUiItems.collectAsState()
    val isHistoryLoading by viewModel.isHistoryLoading.collectAsState()
    val isHistoryRefreshing by viewModel.isHistoryRefreshing.collectAsState()
    val searchQuery by viewModel.searchQueryHistory.collectAsState()

    var selectedItem by remember { mutableStateOf<HistoryUiItem?>(null) }
    var itemToDelete by remember { mutableStateOf<HistoryUiItem?>(null) }
    var deleteFromDisk by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MediumTopAppBar(
                title = { Text(text = stringResource(R.string.history_screen_title)) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateHistorySearchQuery(it) },
                placeholder = { Text(text = stringResource(R.string.history_screen_search_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExpressivePullToRefreshBox(
                isRefreshing = isHistoryRefreshing,
                onRefresh = { viewModel.refreshHistory(isPullToRefresh = true) },
                enabled = !isHistoryLoading,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isHistoryLoading && historyUiItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ExpressiveLoadingIndicator(
                            size = 52.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (historyUiItems.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Text(
                                    text = stringResource(R.string.history_screen_empty_title),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = stringResource(R.string.history_screen_empty_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(
                            items = historyUiItems,
                            key = { _, item -> item.entity.id }
                        ) { index, item ->
                            val total = historyUiItems.size
                            val position = when {
                                total == 1 -> CardGroupPosition.SINGLE
                                index == 0 -> CardGroupPosition.FIRST
                                index == total - 1 -> CardGroupPosition.LAST
                                else -> CardGroupPosition.MIDDLE
                            }

                            val formattedDate = dateFormat.format(Date(item.entity.timestamp))

                            HistoryCardItem(
                                item = item,
                                position = position,
                                formattedDate = formattedDate,
                                onClick = { selectedItem = item }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedItem?.let { item ->
        @Suppress("DEPRECATION")
        val sheetState = rememberModalBottomSheetState()
        val formattedDate = dateFormat.format(Date(item.entity.timestamp))

        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.icon != null) {
                            Image(
                                bitmap = item.icon.toBitmap(52, 52).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(52.dp)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (item.entity.appName.isNotBlank()) item.entity.appName else item.entity.fileName,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (item.entity.packageName.isNotBlank()) item.entity.packageName else item.entity.filePath,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$formattedDate • Key: ${item.entity.keyAlias}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (item.schemes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Schemes: ${item.schemes}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val firstPosShape = getGroupedCardShape(CardGroupPosition.FIRST)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(firstPosShape)
                            .clickable {
                                val targetPath = item.entity.filePath
                                selectedItem = null
                                viewModel.installSignedApk(targetPath)
                            },
                        shape = firstPosShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.history_screen_action_install),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val shareShape = getGroupedCardShape(CardGroupPosition.MIDDLE)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shareShape)
                            .clickable {
                                val targetPath = item.entity.filePath
                                selectedItem = null
                                viewModel.shareSignedFile(targetPath)
                            },
                        shape = shareShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.history_screen_action_share),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val copyShape = getGroupedCardShape(CardGroupPosition.MIDDLE)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(copyShape)
                            .clickable {
                                val targetPath = item.entity.filePath
                                selectedItem = null
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("File Path", targetPath)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, context.getString(R.string.history_screen_path_copied), Toast.LENGTH_SHORT).show()
                            },
                        shape = copyShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.history_screen_action_copy_path),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val deleteShape = getGroupedCardShape(CardGroupPosition.LAST)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(deleteShape)
                            .clickable {
                                itemToDelete = item
                                selectedItem = null
                            },
                        shape = deleteShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.history_screen_action_delete),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(text = stringResource(R.string.history_screen_delete_dialog_title)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.history_screen_delete_dialog_msg))
                    Spacer(modifier = Modifier.height(12.dp))
                    val checkRowShape = RoundedCornerShape(8.dp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(checkRowShape)
                            .clickable { deleteFromDisk = !deleteFromDisk }
                            .padding(4.dp)
                    ) {
                        Checkbox(
                            checked = deleteFromDisk,
                            onCheckedChange = { deleteFromDisk = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.history_screen_delete_file_checkbox),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHistoryItem(item.entity.id, item.entity.filePath, deleteFromDisk)
                        itemToDelete = null
                        deleteFromDisk = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.history_screen_confirm_delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(
                        text = stringResource(R.string.history_screen_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

/**
 * Fast history item card rendering app icon, title, package subtitle, key alias chip, and badges.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistoryCardItem(
    item: HistoryUiItem,
    position: CardGroupPosition,
    formattedDate: String,
    onClick: () -> Unit
) {
    val cardShape = getGroupedCardShape(position)

    val displayTitle = if (item.entity.appName.isNotBlank()) item.entity.appName else item.entity.fileName
    val displaySubtitle = if (item.entity.packageName.isNotBlank()) item.entity.packageName else item.entity.filePath

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable { onClick() },
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (item.icon != null) {
                    Image(
                        bitmap = item.icon.toBitmap(44, 44).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(22.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.height(22.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = item.formattedSize,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.height(22.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.history_screen_key_chip, item.entity.keyAlias),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (item.schemes.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.height(22.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = item.schemes,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (item.isInstalled) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.height(22.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.history_screen_installed_badge),
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

/**
 * Preview composable for HistoryScreen in light and dark themes.
 */
@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    ApkSignerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Text("HistoryScreen Preview", modifier = Modifier.padding(16.dp))
        }
    }
}
