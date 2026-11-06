package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticHelper {
    /**
     * Triggers a subtle tactile response when a strike is logged.
     * Respects user preference in settings.
     */
    fun triggerStrikeHaptic(context: Context, isEnabled: Boolean) {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(30)
                }
            }
        } catch (_: Exception) {
            // Graceful fallback if device lacks hardware vibrator
        }
    }

    /**
     * Crisp, well-tuned haptic feedback when tapping Flame action buttons (Deflame, Girl, etc.).
     */
    fun triggerActionHaptic(context: Context, isEnabled: Boolean) {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(20, 180))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(20)
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Intense, resonant haptic feedback when the flame transitions between major states.
     */
    fun triggerStageTransitionHaptic(context: Context, isEnabled: Boolean) {
        if (!isEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 35, 45, 60)
                    val amplitudes = intArrayOf(0, 190, 0, 255)
                    v?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(70)
                }
            }
        } catch (_: Exception) {
        }
    }
}
