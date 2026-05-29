package com.nlud.civilagesurvey.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.getDefault())
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = true
                }
            }
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "survey_tts")
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
