package com.ameermuawiya.apksigner.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ameermuawiya.apksigner.R
import com.ameermuawiya.apksigner.data.keystore.KeystoreGenParams
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Material 3 Expressive bottom sheet for creating release keystores and cryptographic key pairs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateKeystoreBottomSheet(
    onGenerate: (KeystoreGenParams, (Boolean, String?) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    @Suppress("DEPRECATION")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val focusRequesterFileName = remember { FocusRequester() }
    val focusRequesterPassword = remember { FocusRequester() }
    val focusRequesterConfirmPassword = remember { FocusRequester() }
    val focusRequesterAlias = remember { FocusRequester() }
    val focusRequesterCn = remember { FocusRequester() }
    val focusRequesterOu = remember { FocusRequester() }
    val focusRequesterO = remember { FocusRequester() }
    val focusRequesterL = remember { FocusRequester() }
    val focusRequesterSt = remember { FocusRequester() }
    val focusRequesterCountry = remember { FocusRequester() }

    var fileName by rememberSaveable { mutableStateOf("") }
    var selectedFormat by rememberSaveable { mutableStateOf("PKCS12") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    var alias by rememberSaveable { mutableStateOf("") }
    var selectedAlgorithm by rememberSaveable { mutableStateOf("RSA 2048") }
    var validityYears by rememberSaveable { mutableIntStateOf(30) }
    var selectedExpiryMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    var commonName by rememberSaveable { mutableStateOf("") }
    var organization by rememberSaveable { mutableStateOf("") }
    var organizationalUnit by rememberSaveable { mutableStateOf("") }
    var locality by rememberSaveable { mutableStateOf("") }
    var state by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }

    var submitted by rememberSaveable { mutableStateOf(false) }
    var isGenerating by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

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

    val dateFormat = remember { SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM, Locale.getDefault()) }

    val expiryDisplay = remember(validityYears, selectedExpiryMillis) {
        if (selectedExpiryMillis != null) {
            dateFormat.format(Date(selectedExpiryMillis!!))
        } else {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, validityYears)
            dateFormat.format(cal.time)
        }
    }

    if (showDatePicker) {
        val initialMillis = selectedExpiryMillis ?: remember {
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, validityYears)
            cal.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            if (millis > System.currentTimeMillis()) {
                                selectedExpiryMillis = millis
                                val yearsDiff = ((millis - System.currentTimeMillis()) / (1000L * 60 * 60 * 24 * 365)).toInt()
                                validityYears = yearsDiff.coerceAtLeast(1)
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(text = stringResource(R.string.create_keystore_sheet_date_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.create_keystore_sheet_date_dialog_dismiss))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.create_keystore_sheet_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss, enabled = !isGenerating) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CategoryHeader(title = stringResource(R.string.create_keystore_sheet_cat_file))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val isFileNameErr = submitted && fileName.trim().isBlank()
                TextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        errorMessage = null
                    },
                    label = { Text(text = stringResource(R.string.create_keystore_sheet_filename)) },
                    placeholder = { Text(text = stringResource(R.string.create_keystore_sheet_hint_filename)) },
                    singleLine = true,
                    isError = isFileNameErr,
                    supportingText = if (isFileNameErr) {
                        { Text(text = stringResource(R.string.create_keystore_sheet_err_filename)) }
                    } else null,
                    shape = getGroupedCardShape(CardGroupPosition.FIRST),
                    colors = baseFieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesterPassword.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterFileName)
                )

                Surface(
                    shape = getGroupedCardShape(CardGroupPosition.MIDDLE),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            text = stringResource(R.string.create_keystore_sheet_format),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("PKCS12", "JKS", "BKS").forEach { fmt ->
                                FilterChip(
                                    selected = selectedFormat == fmt,
                                    onClick = { selectedFormat = fmt },
                                    label = { Text(text = fmt) },
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
                }

                val isPasswordErr = submitted && password.length < 6
                TextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text(text = stringResource(R.string.create_keystore_sheet_password)) },
                    placeholder = { Text(text = stringResource(R.string.create_keystore_sheet_hint_password)) },
                    singleLine = true,
                    isError = isPasswordErr,
                    supportingText = if (isPasswordErr) {
                        { Text(text = stringResource(R.string.create_keystore_sheet_err_password_short)) }
                    } else null,
                    shape = getGroupedCardShape(CardGroupPosition.MIDDLE),
                    colors = baseFieldColors,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesterConfirmPassword.requestFocus() }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterPassword)
                )

                val isConfirmErr = submitted && (confirmPassword != password || confirmPassword.isBlank())
                TextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text(text = stringResource(R.string.create_keystore_sheet_confirm_password)) },
                    placeholder = { Text(text = stringResource(R.string.create_keystore_sheet_hint_confirm_password)) },
                    singleLine = true,
                    isError = isConfirmErr,
                    supportingText = if (isConfirmErr) {
                        { Text(text = stringResource(R.string.create_keystore_sheet_err_password_match)) }
                    } else null,
                    shape = getGroupedCardShape(CardGroupPosition.LAST),
                    colors = baseFieldColors,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesterAlias.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterConfirmPassword)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            CategoryHeader(title = stringResource(R.string.create_keystore_sheet_cat_key))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val isAliasErr = submitted && alias.trim().isBlank()
                TextField(
                    value = alias,
                    onValueChange = {
                        alias = it
                        errorMessage = null
                    },
                    label = { Text(text = stringResource(R.string.create_keystore_sheet_alias)) },
                    placeholder = { Text(text = stringResource(R.string.create_keystore_sheet_hint_alias)) },
                    singleLine = true,
                    isError = isAliasErr,
                    supportingText = if (isAliasErr) {
                        { Text(text = stringResource(R.string.create_keystore_sheet_err_alias)) }
                    } else null,
                    shape = getGroupedCardShape(CardGroupPosition.FIRST),
                    colors = baseFieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesterCn.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterAlias)
                )

                Surface(
                    shape = getGroupedCardShape(CardGroupPosition.MIDDLE),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            text = stringResource(R.string.create_keystore_sheet_algorithm),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("RSA 2048", "RSA 4096", "EC secp256r1", "EC secp384r1").forEach { algo ->
                                FilterChip(
                                    selected = selectedAlgorithm == algo,
                                    onClick = { selectedAlgorithm = algo },
                                    label = { Text(text = algo) },
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
                }

                Surface(
                    shape = getGroupedCardShape(CardGroupPosition.LAST),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.create_keystore_sheet_validity_years, validityYears),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.create_keystore_sheet_expiry_display, expiryDisplay),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = stringResource(R.string.create_keystore_sheet_pick_date),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = validityYears == 25 && selectedExpiryMillis == null,
                                onClick = {
                                    validityYears = 25
                                    selectedExpiryMillis = null
                                },
                                label = { Text(text = stringResource(R.string.create_keystore_sheet_preset_25y)) },
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = validityYears == 30 && selectedExpiryMillis == null,
                                onClick = {
                                    validityYears = 30
                                    selectedExpiryMillis = null
                                },
                                label = { Text(text = stringResource(R.string.create_keystore_sheet_preset_30y)) },
                                border = null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            FilterChip(
                                selected = validityYears == 50 && selectedExpiryMillis == null,
                                onClick = {
                                    validityYears = 50
                                    selectedExpiryMillis = null
                                },
                                label = { Text(text = stringResource(R.string.create_keystore_sheet_preset_50y)) },
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            CategoryHeader(title = stringResource(R.string.create_keystore_sheet_cat_cert))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val isCnErr = submitted && commonName.trim().isBlank()
                TextField(
                    value = commonName,
                    onValueChange = {
                        commonName = it
                        errorMessage = null
                    },
                    label = { Text(text = stringResource(R.string.create_keystore_sheet_cn)) },
                    placeholder = { Text(text = stringResource(R.string.create_keystore_sheet_hint_cn)) },
                    singleLine = true,
                    isError = isCnErr,
                    supportingText = if (isCnErr) {
                        { Text(text = stringResource(R.string.create_keystore_sheet_err_cn)) }
                    } else null,
                    shape = getGroupedCardShape(CardGroupPosition.FIRST),
                    colors = baseFieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesterOu.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterCn)
                )

                TextField(
                    value = organizationalUnit,
                    onValueChange = { organizationalUnit = it },
                    label = { Text(text = stringResource(R.string.create_keystore_sheet_ou)) },
                    singleLine = true,
                    shape = getGroupedCardShape(CardGroupPosition.MIDDLE),
                    colors = baseFieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesterO.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterOu)
                )

                TextField(
                    value = organization,
                    onValueChange = { organization = it },
                    label = { Text(text = stringResource(R.string.create_keystore_sheet_o)) },
                    singleLine = true,
                    shape = getGroupedCardShape(CardGroupPosition.MIDDLE),
                    colors = baseFieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesterL.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterO)
                )

                TextField(
                    value = locality,
                    onValueChange = { locality = it },
                    label = { Text(text = stringResource(R.string.create_keystore_sheet_l)) },
                    singleLine = true,
                    shape = getGroupedCardShape(CardGroupPosition.MIDDLE),
                    colors = baseFieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesterSt.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterL)
                )

                TextField(
                    value = state,
                    onValueChange = { state = it },
                    label = { Text(text = stringResource(R.string.create_keystore_sheet_st)) },
                    singleLine = true,
                    shape = getGroupedCardShape(CardGroupPosition.MIDDLE),
                    colors = baseFieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusRequesterCountry.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterSt)
                )

                val isCountryErr = submitted && country.trim().isNotBlank() && country.trim().length != 2
                TextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text(text = stringResource(R.string.create_keystore_sheet_c)) },
                    placeholder = { Text(text = stringResource(R.string.create_keystore_sheet_hint_c)) },
                    singleLine = true,
                    isError = isCountryErr,
                    supportingText = if (isCountryErr) {
                        { Text(text = stringResource(R.string.create_keystore_sheet_err_country)) }
                    } else null,
                    shape = getGroupedCardShape(CardGroupPosition.LAST),
                    colors = baseFieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesterCountry)
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.create_keystore_sheet_err_header),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Keystore Error", errorMessage))
                                    Toast.makeText(context, context.getString(R.string.create_keystore_sheet_err_copied), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.create_keystore_sheet_btn_copy_err),
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        submitted = true
                        val cleanFileName = fileName.trim()
                        val cleanAlias = alias.trim()
                        val cleanPassword = password.trim()
                        val cleanConfirm = confirmPassword.trim()
                        val cleanCn = commonName.trim()
                        val cleanCountry = country.trim()

                        if (cleanFileName.isBlank()) {
                            errorMessage = context.getString(R.string.create_keystore_sheet_err_filename)
                            return@Button
                        }
                        if (cleanPassword.length < 6) {
                            errorMessage = context.getString(R.string.create_keystore_sheet_err_password_short)
                            return@Button
                        }
                        if (cleanPassword != cleanConfirm) {
                            errorMessage = context.getString(R.string.create_keystore_sheet_err_password_match)
                            return@Button
                        }
                        if (cleanAlias.isBlank()) {
                            errorMessage = context.getString(R.string.create_keystore_sheet_err_alias)
                            return@Button
                        }
                        if (cleanCn.isBlank()) {
                            errorMessage = context.getString(R.string.create_keystore_sheet_err_cn)
                            return@Button
                        }
                        if (cleanCountry.isNotBlank() && cleanCountry.length != 2) {
                            errorMessage = context.getString(R.string.create_keystore_sheet_err_country)
                            return@Button
                        }

                        isGenerating = true
                        errorMessage = null

                        val finalExpiryDate = selectedExpiryMillis?.let { Date(it) }

                        val params = KeystoreGenParams(
                            format = selectedFormat,
                            algorithm = selectedAlgorithm,
                            fileName = cleanFileName,
                            alias = cleanAlias,
                            password = cleanPassword,
                            validityYears = validityYears,
                            commonName = cleanCn,
                            organization = organization.trim(),
                            organizationalUnit = organizationalUnit.trim(),
                            locality = locality.trim(),
                            state = state.trim(),
                            country = cleanCountry.uppercase(Locale.ROOT),
                            expiryDate = finalExpiryDate
                        )

                        onGenerate(params) { success, errDetail ->
                            isGenerating = false
                            if (!success) {
                                errorMessage = errDetail ?: context.getString(R.string.create_keystore_sheet_err_header)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.create_keystore_sheet_generating))
                    } else {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.create_keystore_sheet_btn_create))
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = stringResource(R.string.create_keystore_sheet_btn_cancel))
                }
            }
        }
    }
}

/**
 * Section category title within keystore creation bottom sheet.
 */
@Composable
private fun CategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}
