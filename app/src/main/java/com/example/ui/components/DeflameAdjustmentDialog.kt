package com.example.ui.components

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.PlusJakartaSans
import com.example.util.HapticHelper
import java.util.Locale

/**
 * Luxurious Liquid Glass Deflame / Penalty Points Adjustment Dialog.
 * Allows selecting preset deduction options (-2, -3, -5, -10, -25, -50 PTS)
 * AND entering custom penalty points manually via a styled text field.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeflameAdjustmentDialog(
    currentFlameScore: Double,
    onDismiss: () -> Unit,
    onApply: (Double) -> Unit,
    isHapticEnabled: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current

    val presets = listOf(2.0, 3.0, 5.0, 10.0, 25.0, 50.0)
    var selectedPreset by remember { mutableDoubleStateOf(2.0) }
    var textInput by remember { mutableStateOf("2.0") }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        tonalElevation = 12.dp,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x330284C7))
                            .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.deflame_adjustment_title),
                            fontWeight = FontWeight.Bold,
                            fontFamily = PlusJakartaSans,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.deflame_adjustment_subtitle),
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Current Score status pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1838BDF8))
                        .border(1.dp, Color(0x3538BDF8), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.flame_total_points),
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", currentFlameScore)} PTS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = PlusJakartaSans,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets Title
                Text(
                    text = stringResource(R.string.deflame_select_preset),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Quick Preset Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { presetVal ->
                        val isSelected = (selectedPreset == presetVal)
                        val chipText = "-${presetVal.toInt()} PTS"

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF0284C7)
                                    else Color(0x221E293B)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF38BDF8)
                                    else Color(0x4038BDF8),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    selectedPreset = presetVal
                                    textInput = presetVal.toString()
                                    inputError = null
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("deflame_chip_${presetVal.toInt()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chipText,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF38BDF8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Manual Text Input Field
                Text(
                    text = stringResource(R.string.deflame_manual_input_label),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                            textInput = input
                            val parsed = input.toDoubleOrNull()
                            if (parsed != null && parsed > 0.0) {
                                selectedPreset = parsed
                                inputError = null
                            } else if (input.isNotEmpty()) {
                                inputError = "Enter value > 0"
                            }
                        }
                    },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.deflame_manual_input_hint),
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                        }
                    ),
                    isError = inputError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0x4038BDF8),
                        focusedLabelColor = Color(0xFF38BDF8),
                        cursorColor = Color(0xFF38BDF8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("deflame_manual_input")
                )

                if (inputError != null) {
                    Text(
                        text = inputError ?: "",
                        fontSize = 10.sp,
                        color = Color(0xFFFB7185),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val points = textInput.toDoubleOrNull() ?: selectedPreset
                    if (points > 0.0) {
                        HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        onApply(points)
                        onDismiss()
                    } else {
                        inputError = "Enter value > 0"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("deflame_apply_btn")
            ) {
                Text(
                    text = stringResource(R.string.deflame_apply_action),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.flame_cancel_action),
                    color = Color.Gray
                )
            }
        }
    )
}
