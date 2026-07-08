package com.civilagesurvey.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val localeIn = Locale("en", "IN")
            val result = tts.setLanguage(localeIn)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.ENGLISH
            }
            tts.setSpeechRate(0.8f) // Clearer, slower pace (80% rate)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    val voices = tts.voices
                    if (voices != null) {
                        for (voice in voices) {
                            if (voice.locale.country.equals("IN", ignoreCase = true)) {
                                tts.voice = voice
                                break
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isReady = true
        }
    }

    fun speak(text: String) {
        if (isReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun stop() {
        tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
