package com.example.ui.components

import android.view.SoundEffectConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.util.HapticHelper

/**
 * Luxurious containerless 3-stacked-horizontal-lines Settings Menu Button for the Top Banner.
 * Renders three stepped metallic-neon bars directly with a 48dp accessible interactive touch target,
 * ripple & haptic feedback (zero enclosing container or shading).
 */
@Composable
fun LuxurySettingsMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = LocalAppDarkTheme.current,
    isHapticEnabled: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current
    val cdText = stringResource(R.string.settings)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(38.dp)
            .clip(CircleShape)
            .semantics {
                contentDescription = cdText
            }
            .clickable(role = Role.Button) {
                HapticHelper.triggerActionHaptic(context, isHapticEnabled)
                view.playSoundEffect(SoundEffectConstants.CLICK)
                onClick()
            }
            .testTag("top_bar_settings_btn"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val barHeight = 2.4.dp.toPx()
            val cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)

            val topY = h * 0.18f - barHeight / 2f
            val midY = h * 0.50f - barHeight / 2f
            val botY = h * 0.82f - barHeight / 2f

            // Top line: executive full-width glowing bar + subtle gold highlight dot
            val topWidth = w * 0.80f
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = if (isDarkTheme) {
                        listOf(NeonCyan, Color(0xFFA5F3FC))
                    } else {
                        listOf(Color(0xFF0369A1), Color(0xFF0EA5E9))
                    }
                ),
                topLeft = Offset(0f, topY),
                size = Size(topWidth, barHeight),
                cornerRadius = cornerRadius
            )
            drawCircle(
                color = if (isDarkTheme) NeonAmber else Color(0xFFF59E0B),
                radius = barHeight * 0.62f,
                center = Offset(w - barHeight * 0.7f, topY + barHeight / 2f)
            )

            // Middle line: sleekly indented/offset cyber-violet accent bar
            val midWidth = w * 0.76f
            val midStartX = w - midWidth
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color(0xFF818CF8), NeonCyan)
                    } else {
                        listOf(Color(0xFF4F46E5), Color(0xFF0284C7))
                    }
                ),
                topLeft = Offset(midStartX, midY),
                size = Size(midWidth, barHeight),
                cornerRadius = cornerRadius
            )

            // Bottom line: strong anchor bar with luminous tip
            val botWidth = w * 0.90f
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color(0xFF34D399), NeonCyan)
                    } else {
                        listOf(Color(0xFF0F766E), Color(0xFF0EA5E9))
                    }
                ),
                topLeft = Offset(0f, botY),
                size = Size(botWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
