package com.repforge.app.presentation

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import com.repforge.app.RepForgeApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val scope = CoroutineScope(Dispatchers.Main.immediate)

/** Short beep when rest ends; call only while the app is in the foreground. No-op when sounds are disabled. */
fun playRestDone(context: Context) {
    val settings = (context.applicationContext as? RepForgeApplication)?.settings ?: return
    scope.launch(logExceptions) {
        if (!settings.preferences.first().soundsEnabled) return@launch
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
            Handler(Looper.getMainLooper()).postDelayed({ tone.release() }, 500)
        }
    }
}
