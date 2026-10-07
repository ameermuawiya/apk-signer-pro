package com.ameermuawiya.apksigner.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ameermuawiya.apksigner.BuildConfig
import com.ameermuawiya.apksigner.R
import com.ameermuawiya.apksigner.ui.components.CardGroupPosition
import com.ameermuawiya.apksigner.ui.components.CreateKeystoreBottomSheet
import com.ameermuawiya.apksigner.ui.components.KeystoreManagementSheet
import com.ameermuawiya.apksigner.ui.components.KeystorePasswordDialog
import com.ameermuawiya.apksigner.ui.components.getGroupedCardShape
import com.ameermuawiya.apksigner.ui.theme.ApkSignerTheme
import com.ameermuawiya.apksigner.ui.viewmodel.MainViewModel
import com.ameermuawiya.apksigner.utils.UpdateCheckResult
import java.io.File

/**
 * Settings screen configuring themes, keystore profiles, schemes, and working directory location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settingsManager = viewModel.settingsManager

    val themeMode by settingsManager.themeMode.collectAsState()
    val dynamicColor by settingsManager.dynamicColor.collectAsState()
    val workingDir by settingsManager.workingDirectoryPath.collectAsState()
    val v1 by settingsManager.v1Scheme.collectAsState()
    val v2 by settingsManager.v2Scheme.collectAsState()
    val v3 by settingsManager.v3Scheme.collectAsState()
    val v4 by settingsManager.v4Scheme.collectAsState()

    val customKeyPath by settingsManager.customKeyPath.collectAsState()
    val customKeysList by settingsManager.customKeysList.collectAsState()

    val showKeystoreDialog by viewModel.showKeystoreDialog.collectAsState()
    val showGenerateKeyDialog by viewModel.showGenerateKeyDialog.collectAsState()

    var showKeySheet by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (ignored: Exception) {}

            val docId = DocumentsContract.getTreeDocumentId(uri)
            val path = if (docId.startsWith("primary:")) {
                val subPath = docId.substringAfter("primary:")
                val externalDir = Environment.getExternalStorageDirectory()
                File(externalDir, subPath).absolutePath
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloads, "APK Signer Pro").absolutePath
            }

            settingsManager.setWorkingDirectory(path)
            Toast.makeText(context, context.getString(R.string.settings_screen_working_dir_custom_set), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MediumTopAppBar(
                title = { Text(text = stringResource(R.string.settings_screen_title)) },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp)
        ) {
            SectionHeader(title = stringResource(R.string.settings_screen_cat_appearance))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = getGroupedCardShape(CardGroupPosition.FIRST),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Brightness4,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.settings_screen_theme_mode),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = themeMode == "system",
                                onClick = { settingsManager.setThemeMode("system") },
                                label = { Text(text = stringResource(R.string.settings_screen_theme_system)) },
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = themeMode == "light",
                                onClick = { settingsManager.setThemeMode("light") },
                                label = { Text(text = stringResource(R.string.settings_screen_theme_light)) },
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = themeMode == "dark",
                                onClick = { settingsManager.setThemeMode("dark") },
                                label = { Text(text = stringResource(R.string.settings_screen_theme_dark)) },
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

                SettingCardRow(
                    title = stringResource(R.string.settings_screen_dynamic_color),
                    subtitle = stringResource(R.string.settings_screen_dynamic_color_sub),
                    checked = dynamicColor,
                    onCheckedChange = { settingsManager.setDynamicColor(it) },
                    position = CardGroupPosition.LAST,
                    icon = Icons.Default.ColorLens
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SectionHeader(title = stringResource(R.string.settings_screen_cat_key_mgmt))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val activeRecord = if (!customKeyPath.isNullOrBlank()) customKeysList.find { it.path == customKeyPath } else null
                val hasCustomTitle = !activeRecord?.name.isNullOrBlank()
                val activeKeyDesc = if (activeRecord != null) {
                    if (hasCustomTitle) "${activeRecord.name} (Alias: ${activeRecord.alias} • ${activeRecord.format})"
                    else "${activeRecord.alias} (${activeRecord.format})"
                } else {
                    stringResource(R.string.settings_screen_key_default)
                }

                SettingClickableCardRow(
                    title = stringResource(R.string.settings_screen_active_key),
                    subtitle = activeKeyDesc,
                    onClick = { showKeySheet = true },
                    position = CardGroupPosition.FIRST,
                    icon = Icons.Default.Key
                )

                SettingClickableCardRow(
                    title = stringResource(R.string.settings_screen_key_create),
                    subtitle = stringResource(R.string.settings_screen_key_create_desc),
                    onClick = { viewModel.openGenerateKeyDialog() },
                    position = CardGroupPosition.LAST,
                    icon = Icons.Default.VpnKey
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SectionHeader(title = stringResource(R.string.settings_screen_cat_schemes))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SettingCardRow(
                    title = stringResource(R.string.settings_screen_v1_title),
                    subtitle = stringResource(R.string.settings_screen_v1_sub),
                    checked = v1,
                    onCheckedChange = { settingsManager.setV1Scheme(it) },
                    position = CardGroupPosition.FIRST,
                    icon = Icons.Default.Draw
                )

                SettingCardRow(
                    title = stringResource(R.string.settings_screen_v2_title),
                    subtitle = stringResource(R.string.settings_screen_v2_sub),
                    checked = v2,
                    onCheckedChange = { settingsManager.setV2Scheme(it) },
                    position = CardGroupPosition.MIDDLE,
                    icon = Icons.Default.Draw
                )

                SettingCardRow(
                    title = stringResource(R.string.settings_screen_v3_title),
                    subtitle = stringResource(R.string.settings_screen_v3_sub),
                    checked = v3,
                    onCheckedChange = { settingsManager.setV3Scheme(it) },
                    position = CardGroupPosition.MIDDLE,
                    icon = Icons.Default.Draw
                )

                SettingCardRow(
                    title = stringResource(R.string.settings_screen_v4_title),
                    subtitle = stringResource(R.string.settings_screen_v4_sub),
                    checked = v4,
                    onCheckedChange = { settingsManager.setV4Scheme(it) },
                    position = CardGroupPosition.LAST,
                    icon = Icons.Default.Draw
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SectionHeader(title = stringResource(R.string.settings_screen_cat_storage))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SettingClickableCardRow(
                    title = stringResource(R.string.settings_screen_working_dir_title),
                    subtitle = workingDir,
                    onClick = { folderPickerLauncher.launch(null) },
                    position = CardGroupPosition.FIRST,
                    icon = Icons.Default.Folder
                )

                SettingClickableCardRow(
                    title = stringResource(R.string.settings_screen_working_dir_reset_title),
                    subtitle = stringResource(R.string.settings_screen_working_dir_reset_sub),
                    onClick = {
                        settingsManager.resetWorkingDirectory()
                        Toast.makeText(context, context.getString(R.string.settings_screen_working_dir_custom_set), Toast.LENGTH_SHORT).show()
                    },
                    position = CardGroupPosition.LAST,
                    icon = Icons.Default.Refresh
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SectionHeader(title = stringResource(R.string.settings_screen_cat_about))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SettingClickableCardRow(
                    title = stringResource(R.string.settings_screen_about_github_title),
                    subtitle = stringResource(R.string.settings_screen_about_github_sub),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/ameermuawiya/apk-signer-pro")
                        )
                        context.startActivity(intent)
                    },
                    position = CardGroupPosition.FIRST,
                    iconPainter = painterResource(R.drawable.ic_github)
                )

                SettingClickableCardRow(
                    title = stringResource(R.string.settings_screen_about_telegram_title),
                    subtitle = stringResource(R.string.settings_screen_about_telegram_sub),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://t.me/itx_muawiya")
                        )
                        context.startActivity(intent)
                    },
                    position = CardGroupPosition.MIDDLE,
                    iconPainter = painterResource(R.drawable.ic_telegram)
                )

                SettingClickableCardRow(
                    title = stringResource(R.string.settings_screen_about_coffee_title),
                    subtitle = stringResource(R.string.settings_screen_about_coffee_sub),
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.patreon.com/ameermuawiyapk/posts/buy-me-coffee-16891048")
                        )
                        context.startActivity(intent)
                    },
                    position = CardGroupPosition.MIDDLE,
                    iconPainter = painterResource(R.drawable.ic_coffee)
                )

                SettingClickableCardRow(
                    title = stringResource(R.string.settings_screen_about_update_title),
                    subtitle = stringResource(R.string.settings_screen_about_update_sub, BuildConfig.VERSION_NAME),
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.settings_screen_update_checking), Toast.LENGTH_SHORT).show()
                        viewModel.checkForUpdatesManual { result ->
                            when (result) {
                                is UpdateCheckResult.Available -> {
                                    // Update sheet opens automatically through StateFlow
                                }
                                is UpdateCheckResult.UpToDate -> {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_screen_update_up_to_date, BuildConfig.VERSION_NAME),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                is UpdateCheckResult.Error -> {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_screen_update_error),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    position = CardGroupPosition.LAST,
                    icon = Icons.Default.SystemUpdate
                )
            }
        }
    }

    if (showKeySheet) {
        @Suppress("DEPRECATION")
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
            onCreateKeyClick = {
                showKeySheet = false
                viewModel.openGenerateKeyDialog()
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

    if (showGenerateKeyDialog) {
        CreateKeystoreBottomSheet(
            onGenerate = { params, callback ->
                viewModel.generateKeystore(params, callback)
            },
            onDismiss = { viewModel.dismissGenerateKeyDialog() }
        )
    }
}

/**
 * Header text for grouping setting categories.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

/**
 * Reusable switch row card for settings items with properly clipped ripple effects.
 */
@Composable
private fun SettingCardRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    position: CardGroupPosition,
    icon: ImageVector? = null
) {
    val cardShape = getGroupedCardShape(position)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable { onCheckedChange(!checked) },
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

/**
 * Reusable clickable card row for settings navigation and actions.
 */
@Composable
private fun SettingClickableCardRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    position: CardGroupPosition,
    icon: ImageVector? = null,
    iconPainter: Painter? = null
) {
    val cardShape = getGroupedCardShape(position)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable { onClick() },
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null || iconPainter != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (iconPainter != null) {
                            Icon(
                                painter = iconPainter,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Preview composable for SettingsScreen in light and dark themes.
 */
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ApkSignerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Text("SettingsScreen Preview", modifier = Modifier.padding(16.dp))
        }
    }
}
