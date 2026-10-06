package com.ameermuawiya.apksigner.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ameermuawiya.apksigner.R
import com.ameermuawiya.apksigner.data.preferences.CustomKeyRecord

/**
 * Compact Material 3 Expressive bottom sheet displaying keystores with optional custom titles, aliases, and metadata tags.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KeystoreManagementSheet(
    activeKeyPath: String?,
    customKeysList: List<CustomKeyRecord>,
    sheetState: SheetState,
    onSelectKey: (CustomKeyRecord?) -> Unit,
    onDeleteKey: (String) -> Unit,
    onAddKeyClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.keystore_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.keystore_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            val isDefaultSelected = activeKeyPath.isNullOrBlank()
            val defaultShape = if (customKeysList.isEmpty()) {
                getGroupedCardShape(CardGroupPosition.SINGLE)
            } else {
                getGroupedCardShape(CardGroupPosition.FIRST)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(defaultShape)
                        .clickable { onSelectKey(null) },
                    shape = defaultShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isDefaultSelected,
                            onClick = { onSelectKey(null) }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sign_package_screen_key_default),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Alias: androiddebugkey",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                KeyMetadataChip(text = "JKS", isPrimary = true)
                                KeyMetadataChip(text = "RSA 2048")
                                KeyMetadataChip(text = "30 Years")
                            }
                        }
                    }
                }

                customKeysList.forEachIndexed { index, record ->
                    val isSelected = (activeKeyPath == record.path)
                    val pos = if (index == customKeysList.size - 1) {
                        CardGroupPosition.LAST
                    } else {
                        CardGroupPosition.MIDDLE
                    }
                    val cardShape = getGroupedCardShape(pos)
                    val hasCustomTitle = record.name.isNotBlank()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .clickable { onSelectKey(record) },
                        shape = cardShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onSelectKey(record) }
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                if (hasCustomTitle) {
                                    Text(
                                        text = record.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Alias: ${record.alias}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = record.alias,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    KeyMetadataChip(text = record.format, isPrimary = true)
                                    if (record.details.isNotBlank()) {
                                        val subParts = record.details.split("•").map { it.trim() }
                                        subParts.take(3).forEach { part ->
                                            if (part.isNotBlank() &&
                                                !part.equals(record.alias, ignoreCase = true) &&
                                                !part.equals(record.format, ignoreCase = true) &&
                                                !part.equals(record.name, ignoreCase = true)
                                            ) {
                                                KeyMetadataChip(text = part)
                                            }
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onDeleteKey(record.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = stringResource(R.string.sign_package_screen_delete_key),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAddKeyClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.keystore_sheet_btn_add))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Compact informational chip component displaying keystore attributes.
 */
@Composable
private fun KeyMetadataChip(text: String, isPrimary: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isPrimary) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        modifier = Modifier.height(20.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = if (isPrimary) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
