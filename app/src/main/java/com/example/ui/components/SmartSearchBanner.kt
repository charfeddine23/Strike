package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DayCyan
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.PlusJakartaSans

/**
 * Luxurious Liquid Glass Instant & Smart Search Engine Banner
 * replacing the former TOTAL FLAME ENERGY POINTS header container.
 */
@Composable
fun SmartSearchBanner(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    matchedCount: Int,
    totalCount: Int,
    currentLang: String = "ar",
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(16.dp)
    val accentCyan = if (isDarkTheme) NeonCyan else DayCyan
    val borderBrush = Brush.linearGradient(
        colors = if (isFocused || query.isNotBlank()) {
            listOf(accentCyan, NeonViolet, NeonEmerald)
        } else {
            listOf(
                accentCyan.copy(alpha = if (isDarkTheme) 0.55f else 0.45f),
                NeonViolet.copy(alpha = if (isDarkTheme) 0.38f else 0.25f),
                accentCyan.copy(alpha = if (isDarkTheme) 0.55f else 0.45f)
            )
        }
    )

    val bgBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(Color(0xEB0E172A), Color(0xF2080E1B))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color(0xF8FFFFFF), Color(0xF0F1F7FA))
        )
    }

    val placeholderText = when (currentLang) {
        "ar" -> "بحث فوري وذكي (الاسم، الفئة، الهاتف، الضربات...)"
        "fr" -> "Recherche intelligente instantanée (nom, type, tél...)"
        else -> "Instant smart search (name, type, phone, strikes...)"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(56.dp)
            .testTag("smart_search_banner")
            .shadow(
                elevation = if (isFocused || query.isNotBlank()) 8.dp else 4.dp,
                shape = shape,
                ambientColor = accentCyan.copy(alpha = 0.22f),
                spotColor = NeonViolet.copy(alpha = 0.22f)
            )
            .clip(shape)
            .background(bgBrush)
            .border(
                width = 1.25.dp,
                brush = borderBrush,
                shape = shape
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glowing search emblem
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentCyan.copy(alpha = if (isDarkTheme) 0.16f else 0.12f))
                    .border(1.dp, accentCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = placeholderText,
                    tint = accentCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Instant text input field (single-line, vertically locked inside fixed-height banner)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholderText,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 12.sp,
                        fontFamily = PlusJakartaSans,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(accentCyan),
                    textStyle = TextStyle(
                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = PlusJakartaSans
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .testTag("smart_top_search_input")
                )
            }

            // Fixed-height trailing slot so typing/clearing never resizes the banner
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 96.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = query.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (matchedCount > 0) NeonEmerald.copy(alpha = 0.18f)
                                    else Color(0x33EF4444)
                                )
                                .border(
                                    1.dp,
                                    if (matchedCount > 0) NeonEmerald.copy(alpha = 0.6f)
                                    else Color(0x88EF4444),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "$matchedCount/$totalCount",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = PlusJakartaSans,
                                maxLines = 1,
                                color = if (matchedCount > 0) NeonEmerald else Color(0xFFF87171)
                            )
                        }

                        IconButton(
                            onClick = {
                                onClearQuery()
                                focusManager.clearFocus()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("smart_top_search_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = when (currentLang) {
                                    "ar" -> "مسح البحث"
                                    "fr" -> "Effacer"
                                    else -> "Clear search"
                                },
                                tint = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Subtle AI-style Sparkle icon when idle (purely visual cue for Smart Search)
                if (query.isBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonViolet.copy(alpha = if (isDarkTheme) 0.16f else 0.1f))
                            .border(1.dp, NeonViolet.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonViolet,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = when (currentLang) {
                                    "ar" -> "ذكي"
                                    "fr" -> "Smart"
                                    else -> "Smart"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonViolet
                            )
                        }
                    }
                }
            }
        }
    }
}
