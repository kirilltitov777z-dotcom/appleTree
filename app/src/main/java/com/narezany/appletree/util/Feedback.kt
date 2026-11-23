package com.narezany.appletree.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Мягкий отклик: короткий тихий щелчок и лёгкая вибрация. */
class Feedback(private val context: Context) {
    private var tone: ToneGenerator? = null

    private fun tone(): ToneGenerator? {
        if (tone == null) {
            tone = runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, 22)
            }.getOrNull()
        }
        return tone
    }

    fun tap(volumeOn: Boolean, vibrationOn: Boolean, strong: Boolean = false) {
        if (volumeOn) {
            runCatching {
                tone()?.startTone(
                    if (strong) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP,
                    45
                )
            }
        }
        if (vibrationOn) {
            runCatching {
                val vib = if (Build.VERSION.SDK_INT >= 31) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    manager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (strong) {
                    vib.vibrate(VibrationEffect.createOneShot(40, 60))
                } else {
                    vib.vibrate(VibrationEffect.createOneShot(14, 35))
                }
            }
        }
    }

    fun release() {
        runCatching { tone?.release() }
        tone = null
    }
}
