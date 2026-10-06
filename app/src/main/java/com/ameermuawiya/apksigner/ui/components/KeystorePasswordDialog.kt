package com.ameermuawiya.apksigner.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ameermuawiya.apksigner.R
import com.ameermuawiya.apksigner.data.keystore.KeystoreInspectionResult
import java.io.File
import java.io.FileOutputStream

/**
 * Custom keystore import dialog with verification, credential inspection, and optional title note.
 */
@Composable
fun KeystorePasswordDialog(
    onInspectKeystore: (File, String, (Boolean, KeystoreInspectionResult?, String?) -> Unit) -> Unit,
    onSaveKey: (path: String, alias: String, format: String, details: String, password: String, customTitle: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileNameDisplay by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var customTitle by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var inspectionResult by remember { mutableStateOf<KeystoreInspectionResult?>(null) }
    var selectedAlias by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    val titleFocusRequester = remember { FocusRequester() }

    LaunchedEffect(inspectionResult) {
        if (inspectionResult != null) {
            try {
                titleFocusRequester.requestFocus()
            } catch (ignored: Exception) {}
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { targetUri ->
            var displayName = "custom.jks"
            context.contentResolver.query(targetUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
            }
            val destinationFile = File(context.filesDir, "key_${System.currentTimeMillis()}_$displayName")
            context.contentResolver.openInputStream(targetUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            selectedFile = destinationFile
            fileNameDisplay = displayName
            inspectionResult = null
            errorMessage = null
        }
    }

    val baseFieldColors = TextFieldDefaults.colors(
        unfocusedIndicatorColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledLabelColor = MaterialTheme.colorScheme.primary
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        icon = {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = stringResource(R.string.keystore_password_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (inspectionResult == null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        TextField(
                            value = fileNameDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(text = stringResource(R.string.keystore_password_dialog_path_label)) },
                            shape = getGroupedCardShape(CardGroupPosition.FIRST),
                            colors = baseFieldColors,
                            trailingIcon = {
                                IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        TextField(
                            value = password,
                            onValueChange = {
                                password = it
                                inspectionResult = null
                                errorMessage = null
                            },
                            label = { Text(text = stringResource(R.string.keystore_password_dialog_password_label)) },
                            singleLine = true,
                            shape = getGroupedCardShape(CardGroupPosition.LAST),
                            colors = baseFieldColors,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    inspectionResult?.let { result ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.keystore_password_dialog_verified_success),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            TextField(
                                value = customTitle,
                                onValueChange = { customTitle = it },
                                label = { Text(text = stringResource(R.string.keystore_password_dialog_custom_title_label)) },
                                singleLine = true,
                                shape = getGroupedCardShape(CardGroupPosition.FIRST),
                                colors = baseFieldColors,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(titleFocusRequester)
                            )

                            if (result.aliases.size > 1) {
                                TextField(
                                    value = selectedAlias,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(text = stringResource(R.string.keystore_password_dialog_alias_label)) },
                                    shape = getGroupedCardShape(CardGroupPosition.MIDDLE),
                                    colors = baseFieldColors,
                                    trailingIcon = {
                                        IconButton(onClick = { dropdownExpanded = true }) {
                                            Icon(imageVector = Icons.Default.Key, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(getGroupedCardShape(CardGroupPosition.MIDDLE))
                                        .clickable { dropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    result.aliases.forEach { alias ->
                                        DropdownMenuItem(
                                            text = { Text(text = alias) },
                                            onClick = {
                                                selectedAlias = alias
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            } else {
                                ReadOnlyGroupedField(
                                    label = stringResource(R.string.keystore_password_dialog_info_alias),
                                    value = selectedAlias,
                                    position = CardGroupPosition.MIDDLE,
                                    colors = baseFieldColors
                                )
                            }

                            ReadOnlyGroupedField(
                                label = stringResource(R.string.keystore_password_dialog_info_format),
                                value = result.format,
                                position = CardGroupPosition.MIDDLE,
                                colors = baseFieldColors
                            )

                            ReadOnlyGroupedField(
                                label = stringResource(R.string.keystore_password_dialog_info_algorithm),
                                value = result.algorithm,
                                position = CardGroupPosition.MIDDLE,
                                colors = baseFieldColors
                            )

                            ReadOnlyGroupedField(
                                label = stringResource(R.string.keystore_password_dialog_info_validity),
                                value = result.validity,
                                position = CardGroupPosition.MIDDLE,
                                colors = baseFieldColors
                            )

                            ReadOnlyGroupedField(
                                label = stringResource(R.string.keystore_password_dialog_info_subject),
                                value = result.subject,
                                position = CardGroupPosition.MIDDLE,
                                colors = baseFieldColors
                            )

                            ReadOnlyGroupedField(
                                label = stringResource(R.string.keystore_password_dialog_info_fingerprint),
                                value = result.fingerprint,
                                position = CardGroupPosition.LAST,
                                isMonospace = true,
                                colors = baseFieldColors
                            )
                        }
                    }
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.keystore_password_dialog_error_label),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 120.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(
                                        text = err,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Keystore Error", err)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.keystore_password_dialog_error_copied),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.keystore_password_dialog_copy_error),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                shape = RoundedCornerShape(12.dp),
                enabled = !isVerifying,
                onClick = {
                    val file = selectedFile
                    if (file == null) {
                        errorMessage = context.getString(R.string.keystore_password_dialog_no_file)
                        return@Button
                    }
                    if (password.isEmpty()) {
                        errorMessage = context.getString(R.string.keystore_password_dialog_no_pw)
                        return@Button
                    }

                    val currentResult = inspectionResult
                    if (currentResult == null) {
                        isVerifying = true
                        errorMessage = null
                        onInspectKeystore(file, password) { success, result, err ->
                            isVerifying = false
                            if (success && result != null) {
                                inspectionResult = result
                                selectedAlias = result.selectedAlias
                            } else {
                                errorMessage = err ?: context.getString(R.string.keystore_password_dialog_invalid_pw)
                            }
                        }
                    } else {
                        val effectiveAlias = selectedAlias.ifEmpty { currentResult.selectedAlias }
                        onSaveKey(
                            file.absolutePath,
                            effectiveAlias,
                            currentResult.format,
                            currentResult.summary,
                            password,
                            customTitle.trim()
                        )
                    }
                }
            ) {
                Text(
                    text = if (inspectionResult == null) {
                        stringResource(R.string.keystore_password_dialog_verify)
                    } else {
                        stringResource(R.string.keystore_password_dialog_add)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.keystore_password_dialog_cancel))
            }
        }
    )
}

/**
 * Renders non-editable filled text field in Material 3 Expressive grouped list style.
 */
@Composable
private fun ReadOnlyGroupedField(
    label: String,
    value: String,
    position: CardGroupPosition,
    isMonospace: Boolean = false,
    colors: androidx.compose.material3.TextFieldColors
) {
    TextField(
        value = value,
        onValueChange = {},
        enabled = false,
        label = { Text(text = label) },
        shape = getGroupedCardShape(position),
        colors = colors,
        textStyle = if (isMonospace) {
            MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        } else {
            MaterialTheme.typography.bodyMedium
        },
        modifier = Modifier.fillMaxWidth()
    )
}
