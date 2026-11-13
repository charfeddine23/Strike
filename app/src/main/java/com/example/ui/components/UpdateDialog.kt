package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.update.AppUpdateManager
import com.example.data.update.AppUpdateState
import com.example.data.update.GitHubReleaseInfo
import java.io.File

/**
 * Compact, floating Mandatory In-App Update Card Dialog.
 * Matches the dark charcoal aesthetic with the circular crimson rocket emblem,
 * crisp Arabic hierarchy, and full-width vibrant red CTA pill button.
 */
@Composable
fun MandatoryUpdateCardDialog(
    state: AppUpdateState,
    onUpdateClick: (GitHubReleaseInfo) -> Unit,
    onInstallClick: (File) -> Unit,
    onRetryClick: () -> Unit
) {
    val context = LocalContext.current
    val primaryRed = Color(0xFFD80D15)
    val subtitleRed = Color(0xFFE11D25)
    val cardBg = Color(0xFF13131A)
    val mutedText = Color(0xFF9CA3AF)

    Dialog(
        onDismissRequest = { /* Mandatory update — dismissal strictly disabled */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        // Prevent system back button from closing the mandatory update card
        BackHandler(enabled = true) {}

        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .testTag("mandatory_update_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Circular Crimson Rocket Emblem
                RocketUpdateEmblem(
                    modifier = Modifier.size(110.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Main Title
                Text(
                    text = when (state) {
                        is AppUpdateState.Downloading -> "جاري تحميل التحديث"
                        is AppUpdateState.ReadyToInstall -> "التحديث جاهز للتثبيت"
                        is AppUpdateState.Error -> "تعذر إكمال التحديث"
                        else -> "تحديث جديد متاح"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Red Accent Subtitle
                Text(
                    text = when (state) {
                        is AppUpdateState.UpdateAvailable -> "تحديث جديد"
                        is AppUpdateState.Downloading -> "تحديث إجباري • v${state.release.versionName}"
                        is AppUpdateState.ReadyToInstall -> "v${state.release.versionName}"
                        is AppUpdateState.Error -> "تحديث إجباري"
                        else -> "تحديث جديد"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = subtitleRed,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Description / Live Download Progress Area
                when (state) {
                    is AppUpdateState.UpdateAvailable -> {
                        Text(
                            text = "تحديث جديد متاح",
                            fontSize = 14.sp,
                            color = mutedText,
                            textAlign = TextAlign.Center
                        )
                    }

                    is AppUpdateState.Downloading -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val pctText = if (state.progress >= 0f) {
                                "${(state.progress * 100).toInt()}%"
                            } else {
                                "..."
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = pctText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${AppUpdateManager.formatBytes(state.bytesDownloaded)} / ${AppUpdateManager.formatBytes(state.totalBytes)}",
                                    fontSize = 12.sp,
                                    color = mutedText
                                )
                            }
                            if (state.progress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = primaryRed,
                                    trackColor = primaryRed.copy(alpha = 0.22f)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = primaryRed,
                                    trackColor = primaryRed.copy(alpha = 0.22f)
                                )
                            }
                        }
                    }

                    is AppUpdateState.ReadyToInstall -> {
                        Text(
                            text = "تم التحميل بنجاح، اضغط لتثبيت الإصدار الجديد",
                            fontSize = 13.sp,
                            color = mutedText,
                            textAlign = TextAlign.Center
                        )
                    }

                    is AppUpdateState.Error -> {
                        Text(
                            text = state.messageAr,
                            fontSize = 13.sp,
                            color = mutedText,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> Unit
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Full-width Vibrant Red Pill CTA Button
                when (state) {
                    is AppUpdateState.UpdateAvailable -> {
                        Button(
                            onClick = { onUpdateClick(state.release) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("modal_download_update_btn")
                        ) {
                            Text(
                                text = "تحديث",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    is AppUpdateState.Downloading -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                disabledContainerColor = primaryRed.copy(alpha = 0.45f),
                                disabledContentColor = Color.White.copy(alpha = 0.85f)
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "جاري التحميل...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    is AppUpdateState.ReadyToInstall -> {
                        Button(
                            onClick = { onInstallClick(state.apkFile) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("modal_install_update_btn")
                        ) {
                            Text(
                                text = "تثبيت التحديث الآن",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    is AppUpdateState.Error -> {
                        Button(
                            onClick = onRetryClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = primaryRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "إعادة المحاولة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        TextButton(
                            onClick = { AppUpdateManager.openUrlExternally(context, state.fallbackUrl) }
                        ) {
                            Text(
                                text = "فتح رابط التحميل على GitHub",
                                fontSize = 12.sp,
                                color = mutedText
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun RocketUpdateEmblem(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "rocket_flame")
    val flamePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val outerR = w * 0.49f
        val midR = w * 0.40f
        val innerR = w * 0.37f

        // 1. Outer translucent dark red ring
        drawCircle(
            color = Color(0xFF2B0B10),
            radius = outerR,
            center = Offset(cx, cy)
        )

        // 2. Crimson glow border ring
        drawCircle(
            color = Color(0xFFD80D15),
            radius = midR,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.025f)
        )

        // 3. Dark inner circle background
        drawCircle(
            color = Color(0xFF130608),
            radius = innerR,
            center = Offset(cx, cy)
        )

        // 4. Tiny pixel/star particles inside circle
        val dotSize = w * 0.026f
        drawRect(
            color = Color(0xFFD94848),
            topLeft = Offset(cx - innerR * 0.62f, cy - innerR * 0.34f),
            size = Size(dotSize, dotSize)
        )
        drawRect(
            color = Color(0xFFF8FAFC),
            topLeft = Offset(cx + innerR * 0.58f, cy - innerR * 0.42f),
            size = Size(dotSize * 1.15f, dotSize * 1.15f)
        )
        drawRect(
            color = Color(0xFFEAB308),
            topLeft = Offset(cx - innerR * 0.56f, cy + innerR * 0.42f),
            size = Size(dotSize, dotSize)
        )
        drawRect(
            color = Color(0xFFEF4444),
            topLeft = Offset(cx + innerR * 0.52f, cy + innerR * 0.32f),
            size = Size(dotSize, dotSize)
        )

        // 5. Rocket Thruster Exhaust Flame (animated)
        val flameTopY = cy + innerR * 0.43f
        val flameTipY = flameTopY + innerR * 0.34f * flamePulse
        val flameOuterPath = Path().apply {
            moveTo(cx - innerR * 0.13f, flameTopY)
            lineTo(cx + innerR * 0.13f, flameTopY)
            lineTo(cx, flameTipY)
            close()
        }
        drawPath(
            path = flameOuterPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFD000), Color(0xFFFF4500)),
                startY = flameTopY,
                endY = flameTipY
            )
        )
        val flameCorePath = Path().apply {
            moveTo(cx - innerR * 0.06f, flameTopY)
            lineTo(cx + innerR * 0.06f, flameTopY)
            lineTo(cx, flameTopY + (flameTipY - flameTopY) * 0.62f)
            close()
        }
        drawPath(path = flameCorePath, color = Color(0xFFFFF3B0))

        // 6. Left & Right Crimson Fins
        val finTopY = cy + innerR * 0.05f
        val finBottomY = cy + innerR * 0.42f
        val leftFin = Path().apply {
            moveTo(cx - innerR * 0.17f, finTopY)
            lineTo(cx - innerR * 0.44f, finBottomY)
            lineTo(cx - innerR * 0.15f, finBottomY - innerR * 0.08f)
            close()
        }
        val rightFin = Path().apply {
            moveTo(cx + innerR * 0.17f, finTopY)
            lineTo(cx + innerR * 0.44f, finBottomY)
            lineTo(cx + innerR * 0.15f, finBottomY - innerR * 0.08f)
            close()
        }
        drawPath(path = leftFin, color = Color(0xFFB8111A))
        drawPath(path = rightFin, color = Color(0xFFB8111A))

        // 7. Rocket Nozzle Base
        drawRoundRect(
            color = Color(0xFF475569),
            topLeft = Offset(cx - innerR * 0.12f, cy + innerR * 0.36f),
            size = Size(innerR * 0.24f, innerR * 0.09f)
        )

        // 8. White Rocket Fuselage + Red Nose Cone
        val rocketTopY = cy - innerR * 0.68f
        val noseJoinY = cy - innerR * 0.34f
        val bodyBottomY = cy + innerR * 0.38f

        val fuselagePath = Path().apply {
            moveTo(cx, rocketTopY)
            quadraticTo(cx + innerR * 0.26f, noseJoinY, cx + innerR * 0.18f, bodyBottomY)
            lineTo(cx - innerR * 0.18f, bodyBottomY)
            quadraticTo(cx - innerR * 0.26f, noseJoinY, cx, rocketTopY)
            close()
        }
        drawPath(path = fuselagePath, color = Color(0xFFF8FAFC))

        // Red Nose Cone Overlay
        val nosePath = Path().apply {
            moveTo(cx, rocketTopY)
            quadraticTo(cx + innerR * 0.20f, (rocketTopY + noseJoinY) * 0.55f, cx + innerR * 0.16f, noseJoinY)
            quadraticTo(cx, noseJoinY + innerR * 0.04f, cx - innerR * 0.16f, noseJoinY)
            quadraticTo(cx - innerR * 0.20f, (rocketTopY + noseJoinY) * 0.55f, cx, rocketTopY)
            close()
        }
        drawPath(path = nosePath, color = Color(0xFFD80D15))

        // 9. Cyan Porthole Window
        val windowCenter = Offset(cx, cy - innerR * 0.06f)
        drawCircle(
            color = Color(0xFF0F172A),
            radius = innerR * 0.145f,
            center = windowCenter
        )
        drawCircle(
            color = Color(0xFF22D3EE),
            radius = innerR * 0.105f,
            center = windowCenter
        )
        // Tiny window highlight arc
        drawArc(
            color = Color.White.copy(alpha = 0.7f),
            startAngle = 200f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(windowCenter.x - innerR * 0.07f, windowCenter.y - innerR * 0.07f),
            size = Size(innerR * 0.14f, innerR * 0.14f),
            style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
        )
    }
}
