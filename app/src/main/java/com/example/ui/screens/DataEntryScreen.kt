package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Individual
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.viewmodel.IndividualViewModel

@Composable
fun DataEntryScreen(
    viewModel: IndividualViewModel,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name by viewModel.formName.collectAsStateWithLifecycle()
    val type by viewModel.formType.collectAsStateWithLifecycle()
    val length by viewModel.formLength.collectAsStateWithLifecycle()
    val width by viewModel.formWidth.collectAsStateWithLifecycle()
    val sScore by viewModel.formSScore.collectAsStateWithLifecycle()
    val fScore by viewModel.formFScore.collectAsStateWithLifecycle()
    val cScore by viewModel.formCScore.collectAsStateWithLifecycle()
    val error by viewModel.formError.collectAsStateWithLifecycle()

    val calculatedSize by viewModel.liveCalculatedSize.collectAsStateWithLifecycle()
    val calculatedBoost by viewModel.liveCalculatedBoost.collectAsStateWithLifecycle()

    val isEditing = viewModel.editingId != null

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Mode Header
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = if (isEditing) stringResource(R.string.update_record) else stringResource(R.string.nav_entry),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.auto_calculated),
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isEditing) NeonViolet.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.2f))
                            .border(1.dp, if (isEditing) NeonViolet else NeonCyan, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isEditing) "EDIT MODE" else "NEW ENTRY",
                            color = if (isEditing) NeonViolet else NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Error message banner if any
        if (error != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonRose.copy(alpha = 0.15f))
                        .border(1.dp, NeonRose.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = NeonRose,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error!!,
                        color = NeonRose,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Section 1: Identity & Type
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.full_name),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.formName.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_fullname"),
                        placeholder = { Text(stringResource(R.string.full_name_hint)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0x3338BDF8),
                            focusedContainerColor = Color(0x151E293B),
                            unfocusedContainerColor = Color(0x0C1E293B)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.type),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Type Selector Buttons ("M", "G", "TS", "FG")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Individual.VALID_TYPES.forEach { opt ->
                            val isSelected = type == opt
                            val activeBg = when (opt) {
                                "M" -> NeonCyan
                                "G" -> NeonViolet
                                "TS" -> NeonEmerald
                                else -> NeonAmber
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) activeBg.copy(alpha = 0.25f) else Color(0x12FFFFFF))
                                    .border(
                                        1.5.dp,
                                        if (isSelected) activeBg else Color(0x2538BDF8),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.formType.value = opt }
                                    .padding(vertical = 10.dp)
                                    .testTag("type_selector_$opt"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    color = if (isSelected) activeBg else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Dimensions & Live Size Calculation
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DIMENSIONS & SIZE",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.formula_size),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = length,
                            onValueChange = { viewModel.formLength.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_length"),
                            label = { Text(stringResource(R.string.length)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x3338BDF8)
                            )
                        )

                        OutlinedTextField(
                            value = width,
                            onValueChange = { viewModel.formWidth.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_width"),
                            label = { Text(stringResource(R.string.width)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x3338BDF8)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Calculated Size Preview Box
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeonCyan.copy(alpha = 0.15f), Color(0x1038BDF8))
                                )
                            )
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.size),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$length × $width",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Text(
                            text = "= $calculatedSize",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan,
                            modifier = Modifier.testTag("preview_live_size")
                        )
                    }
                }
            }
        }

        // Section 3: Scores & Live BOOST Calculation
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.scores_breakdown),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.formula_boost),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sScore,
                            onValueChange = { viewModel.formSScore.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_sscore"),
                            label = { Text("S") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0x3338BDF8)
                            )
                        )

                        OutlinedTextField(
                            value = fScore,
                            onValueChange = { viewModel.formFScore.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_fscore"),
                            label = { Text("F") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonViolet,
                                unfocusedBorderColor = Color(0x33A855F7)
                            )
                        )

                        OutlinedTextField(
                            value = cScore,
                            onValueChange = { viewModel.formCScore.value = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_cscore"),
                            label = { Text("C") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonAmber,
                                unfocusedBorderColor = Color(0x33F59E0B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Calculated BOOST Preview Box with Formula Breakdown
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeonEmerald.copy(alpha = 0.2f), NeonCyan.copy(alpha = 0.15f))
                                )
                            )
                            .border(1.dp, NeonEmerald.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DYNAMIC BOOST",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "$calculatedBoost",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonEmerald,
                                modifier = Modifier.testTag("preview_live_boost")
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Breakdown tags
                        val s = sScore.toDoubleOrNull() ?: 0.0
                        val f = fScore.toDoubleOrNull() ?: 0.0
                        val c = cScore.toDoubleOrNull() ?: 0.0
                        val sumScores = ((s + f + c) * 100.0).toLong() / 100.0
                        val sizeBonus = ((calculatedSize / 100.0) * 100.0).toLong() / 100.0

                        Text(
                            text = "($sumScores sum) + ($sizeBonus size/100) = $calculatedBoost",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Action Buttons: Save & Cancel
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onFinished,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("cancel_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = {
                        viewModel.saveCurrentForm {
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp)
                        .testTag("save_record_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF021B26)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) stringResource(R.string.update_record) else stringResource(R.string.save_record),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
