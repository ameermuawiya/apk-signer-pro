package com.ameermuawiya.apksigner.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
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
import com.ameermuawiya.apksigner.data.model.InstalledAppInfo
import com.ameermuawiya.apksigner.ui.components.AppUpdateBottomSheet
import com.ameermuawiya.apksigner.ui.components.CardGroupPosition
import com.ameermuawiya.apksigner.ui.components.ExpressiveLoadingIndicator
import com.ameermuawiya.apksigner.ui.components.ExpressivePullToRefreshBox
import com.ameermuawiya.apksigner.ui.components.FullScreenLoadingOverlay
import com.ameermuawiya.apksigner.ui.components.getGroupedCardShape
import com.ameermuawiya.apksigner.ui.theme.ApkSignerTheme
import com.ameermuawiya.apksigner.ui.viewmodel.MainViewModel

/**
 * Main dashboard screen displaying installed applications, tab categorization, and quick package signing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val installedApps by viewModel.filteredInstalledApps.collectAsState()
    val isInitialLoading by viewModel.isAppsLoading.collectAsState()
    val isAppsRefreshing by viewModel.isAppsRefreshing.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val searchQuery by viewModel.searchQueryHome.collectAsState()
    val activeTabIndex by viewModel.homeTabIndex.collectAsState()
    val availableUpdate by viewModel.availableUpdate.collectAsState()

    var selectedAppForOptions by remember { mutableStateOf<InstalledAppInfo?>(null) }

    val userAppsListState = rememberLazyListState()
    val systemAppsListState = rememberLazyListState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectFileFromUri(it) }
    }

    val userApps = remember(installedApps) { installedApps.filter { !it.isSystemApp } }
    val systemApps = remember(installedApps) { installedApps.filter { it.isSystemApp } }
    val displayedApps = if (activeTabIndex == 0) userApps else systemApps

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MediumTopAppBar(
                title = { Text(text = stringResource(R.string.home_screen_title)) },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/vnd.android.package-archive",
                            "application/zip",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_screen_select_file)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateHomeSearchQuery(it) },
                    placeholder = { Text(text = stringResource(R.string.home_screen_search_hint)) },
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

                PrimaryTabRow(
                    selectedTabIndex = activeTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = {
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(activeTabIndex),
                            width = 36.dp,
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = activeTabIndex == 0,
                        onClick = { viewModel.setHomeTabIndex(0) },
                        text = { Text(text = stringResource(R.string.home_screen_tab_user_apps)) }
                    )
                    Tab(
                        selected = activeTabIndex == 1,
                        onClick = { viewModel.setHomeTabIndex(1) },
                        text = { Text(text = stringResource(R.string.home_screen_tab_system_apps)) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                ExpressivePullToRefreshBox(
                    isRefreshing = isAppsRefreshing,
                    onRefresh = { viewModel.loadInstalledApps(isPullToRefresh = true) },
                    enabled = !isInitialLoading,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isInitialLoading && installedApps.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ExpressiveLoadingIndicator(
                                size = 52.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (displayedApps.isEmpty()) {
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
                                                imageVector = Icons.Default.Apps,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Text(
                                        text = stringResource(R.string.home_screen_empty_title),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = stringResource(R.string.home_screen_empty_subtitle),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = if (activeTabIndex == 0) userAppsListState else systemAppsListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 2.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(
                                items = displayedApps,
                                key = { _, item -> item.packageName }
                            ) { index, app ->
                                val total = displayedApps.size
                                val position = when {
                                    total == 1 -> CardGroupPosition.SINGLE
                                    index == 0 -> CardGroupPosition.FIRST
                                    index == total - 1 -> CardGroupPosition.LAST
                                    else -> CardGroupPosition.MIDDLE
                                }

                                InstalledAppCardItem(
                                    app = app,
                                    position = position,
                                    onClick = { viewModel.selectInstalledApp(app) },
                                    onLongClick = { selectedAppForOptions = app }
                                )
                            }
                        }
                    }
                }
            }
        }

        FullScreenLoadingOverlay(
            visible = isBackingUp
        )
    }

    selectedAppForOptions?.let { app ->
        @Suppress("DEPRECATION")
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedAppForOptions = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_screen_action_dialog_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon.toBitmap(48, 48).asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "v${app.versionName} • ${app.formattedSize}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val signShape = getGroupedCardShape(CardGroupPosition.FIRST)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(signShape)
                            .clickable {
                                selectedAppForOptions = null
                                viewModel.selectInstalledApp(app)
                            },
                        shape = signShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.home_screen_action_sign),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val extractShape = getGroupedCardShape(CardGroupPosition.MIDDLE)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(extractShape)
                            .clickable {
                                selectedAppForOptions = null
                                viewModel.backupInstalledApp(
                                    appInfo = app,
                                    onComplete = { success, result ->
                                        if (success) {
                                            val fileName = java.io.File(result).name
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.home_screen_extracted_success, fileName),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.home_screen_extracted_fail),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                )
                            },
                        shape = extractShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.home_screen_action_extract),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    val shareShape = getGroupedCardShape(CardGroupPosition.LAST)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shareShape)
                            .clickable {
                                selectedAppForOptions = null
                                viewModel.shareSourceApk(app)
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
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.home_screen_action_share),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    availableUpdate?.let { updateInfo ->
        val updateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        AppUpdateBottomSheet(
            updateInfo = updateInfo,
            sheetState = updateSheetState,
            onDismiss = { viewModel.dismissUpdateSheet() }
        )
    }
}

/**
 * Fast item card component for installed application in user or system tabs.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun InstalledAppCardItem(
    app: InstalledAppInfo,
    position: CardGroupPosition,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cardShape = getGroupedCardShape(position)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
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
                if (app.icon != null) {
                    Image(
                        bitmap = app.icon.toBitmap(44, 44).asImageBitmap(),
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
                                imageVector = Icons.Default.Android,
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
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = app.packageName,
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
                                text = "v${app.versionName}",
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
                                text = app.formattedSize,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    if (app.splitApkFiles.isNotEmpty()) {
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
                                    text = "Split (${app.splitApkFiles.size + 1})",
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
 * Preview composable for HomeScreen in light and dark themes.
 */
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    ApkSignerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Text("HomeScreen Preview", modifier = Modifier.padding(16.dp))
        }
    }
}
