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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
 * Compact, floating In-App Update Card Dialog.
 * Defaults to optional mode (user can skip with "Maybe Later", close button, back press, or outside tap).
 */
@Composable
fun AppUpdateCardDialog(
    state: AppUpdateState,
    onUpdateClick: (GitHubReleaseInfo) -> Unit,
    onInstallClick: (File) -> Unit,
    onRetryClick: () -> Unit,
    onDismiss: () -> Unit,
    onCancelDownload: () -> Unit = {},
    currentLang: String = "ar",
    isMandatory: Boolean = false
) {
    val context = LocalContext.current
    val isAr = currentLang == "ar"
    val isFr = currentLang == "fr"
    val primaryRed = Color(0xFFD80D15)
    val subtitleRed = Color(0xFFE11D25)
    val cardBg = Color(0xFF13131A)
    val mutedText = Color(0xFF9CA3AF)
    val isDownloading = state is AppUpdateState.Downloading

    val laterLabel = when {
        isAr -> "ربما لاحقاً"
        isFr -> "Plus tard"
        else -> "Maybe Later"
    }

    BackHandler(enabled = isMandatory || isDownloading) {
        if (!isMandatory && isDownloading) {
            onCancelDownload()
        }
    }

    Dialog(
        onDismissRequest = { if (!isMandatory && !isDownloading) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isMandatory && !isDownloading,
            dismissOnClickOutside = !isMandatory && !isDownloading
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(cardBg)
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .testTag(if (isMandatory) "mandatory_update_dialog" else "optional_update_dialog")
        ) {
            if (!isMandatory && !isDownloading) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .minimumInteractiveComponentSize()
                        .clip(CircleShape)
                        .background(Color(0x18FFFFFF))
                        .testTag("modal_close_update_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = laterLabel,
                        tint = mutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

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
                        is AppUpdateState.Downloading -> when {
                            isAr -> "جاري تحميل التحديث"
                            isFr -> "Téléchargement de la mise à jour"
                            else -> "Downloading Update"
                        }
                        is AppUpdateState.ReadyToInstall -> when {
                            isAr -> "التحديث جاهز للتثبيت"
                            isFr -> "Mise à jour prête à installer"
                            else -> "Update Ready to Install"
                        }
                        is AppUpdateState.Error -> when {
                            isAr -> "تعذر إكمال التحديث"
                            isFr -> "Échec de la mise à jour"
                            else -> "Update Failed"
                        }
                        else -> when {
                            isMandatory && isAr -> "تحديث إجباري متاح"
                            isMandatory && isFr -> "Mise à jour obligatoire"
                            isMandatory -> "Mandatory Update Required"
                            isAr -> "تحديث جديد متاح"
                            isFr -> "Nouvelle mise à jour disponible"
                            else -> "New Update Available"
                        }
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
                        is AppUpdateState.UpdateAvailable -> when {
                            isAr -> "إصدار جديد • v${state.release.versionName}"
                            isFr -> "Nouvelle version • v${state.release.versionName}"
                            else -> "New Version • v${state.release.versionName}"
                        }
                        is AppUpdateState.Downloading -> when {
                            isAr -> "تحديث جديد • v${state.release.versionName}"
                            isFr -> "Téléchargement • v${state.release.versionName}"
                            else -> "Downloading • v${state.release.versionName}"
                        }
                        is AppUpdateState.ReadyToInstall -> "v${state.release.versionName}"
                        is AppUpdateState.Error -> when {
                            isMandatory && isAr -> "تحديث إجباري"
                            isMandatory && isFr -> "Mise à jour obligatoire"
                            isMandatory -> "Mandatory Update"
                            isAr -> "تعذر تنزيل التحديث"
                            isFr -> "Erreur de mise à jour"
                            else -> "Update Error"
                        }
                        else -> when {
                            isAr -> "تحديث جديد"
                            isFr -> "Nouvelle mise à jour"
                            else -> "New Update"
                        }
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
                            text = when {
                                isMandatory && isAr -> "يجب تحديث التطبيق للاستمرار في الاستخدام"
                                isMandatory && isFr -> "Vous devez mettre à jour l'application pour continuer"
                                isMandatory -> "Please update the app to continue using it"
                                isAr -> "يتوفر تحديث اختياري جديد على GitHub؛ يمكنك التنزيل الآن أو المتابعة لاحقاً"
                                isFr -> "Une mise à jour optionnelle est disponible sur GitHub"
                                else -> "An optional update is available on GitHub. Download now or continue later."
                            },
                            fontSize = 14.sp,
                            color = mutedText,
                            textAlign = TextAlign.Center
                        )
                        if (state.release.apkSizeBytes > 0L) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when {
                                    isAr -> "الحجم: ${AppUpdateManager.formatBytes(state.release.apkSizeBytes)}"
                                    isFr -> "Taille : ${AppUpdateManager.formatBytes(state.release.apkSizeBytes)}"
                                    else -> "Size: ${AppUpdateManager.formatBytes(state.release.apkSizeBytes)}"
                                },
                                fontSize = 12.sp,
                                color = mutedText.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
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
                            text = when {
                                isAr -> "تم التحميل بنجاح، اضغط لتثبيت الإصدار الجديد"
                                isFr -> "Téléchargé avec succès. Appuyez pour installer la nouvelle version."
                                else -> "Downloaded successfully. Tap below to install the new version."
                            },
                            fontSize = 13.sp,
                            color = mutedText,
                            textAlign = TextAlign.Center
                        )
                    }

                    is AppUpdateState.Error -> {
                        Text(
                            text = state.getLocalizedMessage(currentLang),
                            fontSize = 13.sp,
                            color = mutedText,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> Unit
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Full-width Vibrant Red Pill CTA Button + Optional Later Button
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
                                text = when {
                                    isAr -> "تحديث"
                                    isFr -> "Mettre à jour"
                                    else -> "Update Now"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        if (!isMandatory) {
                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("modal_maybe_later_btn")
                            ) {
                                Text(
                                    text = laterLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = mutedText
                                )
                            }
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
                                text = when {
                                    isAr -> "جاري التحميل..."
                                    isFr -> "Téléchargement..."
                                    else -> "Downloading..."
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (!isMandatory) {
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = onCancelDownload,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("modal_cancel_download_btn")
                            ) {
                                Text(
                                    text = when {
                                        isAr -> "إلغاء التحميل"
                                        isFr -> "Annuler le téléchargement"
                                        else -> "Cancel Download"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = mutedText
                                )
                            }
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
                                text = when {
                                    isAr -> "تثبيت التحديث الآن"
                                    isFr -> "Installer maintenant"
                                    else -> "Install Update Now"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        if (!isMandatory) {
                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("modal_maybe_later_btn")
                            ) {
                                Text(
                                    text = laterLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = mutedText
                                )
                            }
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
                                text = when {
                                    isAr -> "إعادة المحاولة"
                                    isFr -> "Réessayer"
                                    else -> "Retry"
                                },
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
                                text = when {
                                    isAr -> "فتح رابط التحميل على GitHub"
                                    isFr -> "Ouvrir le lien sur GitHub"
                                    else -> "Open GitHub Download Link"
                                },
                                fontSize = 12.sp,
                                color = mutedText
                            )
                        }

                        if (!isMandatory) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("modal_maybe_later_btn")
                            ) {
                                Text(
                                    text = laterLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = mutedText
                                )
                            }
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
