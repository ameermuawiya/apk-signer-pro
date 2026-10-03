package com.ameermuawiya.apksigner.ui.components

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ameermuawiya.apksigner.R
import com.ameermuawiya.apksigner.data.keystore.KeystoreInspectionResult
import com.ameermuawiya.apksigner.ui.theme.ApkSignerTheme
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

    val textFieldColors = TextFieldDefaults.colors(
        unfocusedIndicatorColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                TextField(
                    value = fileNameDisplay,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(text = stringResource(R.string.keystore_password_dialog_path_label)) },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    trailingIcon = {
                        IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        inspectionResult = null
                        errorMessage = null
                    },
                    label = { Text(text = stringResource(R.string.keystore_password_dialog_password_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
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

                inspectionResult?.let { result ->
                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
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
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextField(
                        value = customTitle,
                        onValueChange = { customTitle = it },
                        label = { Text(text = stringResource(R.string.keystore_password_dialog_custom_title_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (result.aliases.size > 1) {
                        TextField(
                            value = selectedAlias,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(text = stringResource(R.string.keystore_password_dialog_alias_label)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = true }) {
                                    Icon(imageVector = Icons.Default.Key, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
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
                        ReadOnlyInfoField(
                            label = stringResource(R.string.keystore_password_dialog_info_alias),
                            value = selectedAlias,
                            colors = textFieldColors
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ReadOnlyInfoField(
                        label = stringResource(R.string.keystore_password_dialog_info_format),
                        value = result.format,
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ReadOnlyInfoField(
                        label = stringResource(R.string.keystore_password_dialog_info_algorithm),
                        value = result.algorithm,
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ReadOnlyInfoField(
                        label = stringResource(R.string.keystore_password_dialog_info_validity),
                        value = result.validity,
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ReadOnlyInfoField(
                        label = stringResource(R.string.keystore_password_dialog_info_subject),
                        value = result.subject,
                        colors = textFieldColors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ReadOnlyInfoField(
                        label = stringResource(R.string.keystore_password_dialog_info_fingerprint),
                        value = result.fingerprint,
                        isMonospace = true,
                        colors = textFieldColors
                    )
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
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

                    if (inspectionResult == null) {
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
                        val result = inspectionResult!!
                        val effectiveAlias = selectedAlias.ifEmpty { result.selectedAlias }
                        onSaveKey(
                            file.absolutePath,
                            effectiveAlias,
                            result.format,
                            result.summary,
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
 * Renders non-editable read-only metadata text field.
 */
@Composable
private fun ReadOnlyInfoField(
    label: String,
    value: String,
    isMonospace: Boolean = false,
    colors: androidx.compose.material3.TextFieldColors
) {
    TextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(text = label) },
        shape = RoundedCornerShape(12.dp),
        colors = colors,
        textStyle = if (isMonospace) {
            MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        } else {
            MaterialTheme.typography.bodyMedium
        },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Preview composable for KeystorePasswordDialog in light and dark themes.
 */
@Preview(showBackground = true)
@Composable
fun KeystorePasswordDialogPreview() {
    ApkSignerTheme {
        Text("KeystorePasswordDialog Preview")
    }
}
