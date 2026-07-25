package com.civilagesurvey.app.audio

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.*

class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false
    private var nextHindiText: String? = null

    private val hindiTranslations = mapOf(
        "What is your current age?" to "आपकी वर्तमान आयु क्या है?",
        "What is your gender?" to "आपका लिंग क्या है?",
        "What is your mother tongue / primary language?" to "आपकी मातृभाषा या प्राथमिक भाषा क्या है?",
        "What is the highest level of formal education you have completed?" to "आपने औपचारिक शिक्षा का उच्चतम स्तर क्या पूरा किया है?",
        "What is your current living arrangement?" to "आपकी वर्तमान रहने की व्यवस्था क्या है?",
        "What is your primary source of monthly income?" to "आपकी मासिक आय का प्राथमिक स्रोत क्या है?",
        "What is your estimated total monthly household income (including all sources)?" to "आपकी अनुमानित कुल मासिक घरेलू आय क्या है?",
        "How long have you been living in this city?" to "आप इस शहर में कब से रह रहे हैं?",
        "How would you describe your general health these days?" to "आजकल आप अपने सामान्य स्वास्थ्य का वर्णन कैसे करेंगे?",
        "Do you currently experience any of the following? (Select all that apply)" to "क्या आप वर्तमान में इनमें से किसी का अनुभव करते हैं? सभी उपयुक्त विकल्पों का चयन करें।",
        "Do you currently receive regular caregiving support from a paid or unpaid caregiver?" to "क्या आपको वर्तमान में किसी भुगतान या मुफ्त देखभाल करने वाले से नियमित सहायता मिलती है?",
        "Do you personally own a mobile phone?" to "क्या आपके पास व्यक्तिगत रूप से मोबाइल फोन है?",
        "If you have access to any phone, how do you primarily use it?" to "यदि आपकी किसी फोन तक पहुंच है, तो आप मुख्य रूप से इसका उपयोग कैसे करते हैं?",
        "For digital tasks on a phone or computer, who usually operates the device for you?" to "फोन या कंप्यूटर पर डिजिटल कार्यों के लिए, आमतौर पर आपके लिए डिवाइस कौन चलाता है?",
        "Do you have access to a computer, laptop, or tablet at home?" to "क्या आपके पास घर पर कंप्यूटर, लैपटॉप या टैबलेट की पहुंच है?",
        "How often do you PERSONALLY attempt to use any digital device - phone, computer, digital kiosk - without asking for help first?" to "आप व्यक्तिगत रूप से किसी डिजिटल डिवाइस जैसे फोन, कंप्यूटर, डिजिटल कियोस्क का उपयोग करने का कितनी बार प्रयास करते हैं?",
        "Before your retirement or in your working life, did you ever regularly use any digital device or technology?" to "अपनी सेवानिवृत्ति से पहले या अपने कामकाजी जीवन में, क्या आपने कभी नियमित रूप से किसी डिजिटल डिवाइस या तकनीक का उपयोग किया था?",
        "On a scale of 1 to 5, how confident do you feel right now in using a smartphone or a digital kiosk to complete a simple task?" to "एक से पांच के पैमाने पर, आप एक साधारण कार्य को पूरा करने के लिए स्मार्टफोन या डिजिटल कियोस्क का उपयोग करने में कितना आश्वस्त महसूस करते हैं?",
        "Is there a working internet connection available in your home or old age home?" to "क्या आपके घर या वृद्धाश्रम में काम करने वाला इंटरनेट कनेक्शन उपलब्ध है?",
        "Smart City Service Usage Grid" to "स्मार्ट सिटी सेवा उपयोग ग्रिड",
        "Socio-Demographic Profile" to "सामाजिक-जनसांख्यिकीय प्रोफ़ाइल",
        "Digital Device Ownership and Exposure Profile" to "डिजिटल डिवाइस स्वामित्व और एक्सपोजर प्रोफ़ाइल",
        "Awareness and Usage of Smart City Mission Services" to "स्मार्ट सिटी मिशन सेवाओं की जागरूकता और उपयोग"
    )

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setupTtsSettings()
            setupProgressListener()
            isReady = true
        }
    }

    private fun setupTtsSettings() {
        val localeIn = Locale("en", "IN")
        val result = tts.setLanguage(localeIn)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.language = Locale.ENGLISH
        }
        tts.setSpeechRate(0.8f) // Clearer, slower pace
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                val voices = tts.voices
                if (voices != null) {
                    for (voice in voices) {
                        if (voice.locale.country.equals("IN", ignoreCase = true) && 
                            voice.locale.language.equals("en", ignoreCase = true)) {
                            tts.voice = voice
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupProgressListener() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                if (utteranceId == "english_speech") {
                    nextHindiText?.let { hindiText ->
                        nextHindiText = null
                        Handler(Looper.getMainLooper()).post {
                            try {
                                tts.language = Locale("hi", "IN")
                                val params = Bundle()
                                params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "hindi_speech")
                                tts.speak(hindiText, TextToSpeech.QUEUE_ADD, params, "hindi_speech")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                } else if (utteranceId == "hindi_speech") {
                    Handler(Looper.getMainLooper()).post {
                        try {
                            tts.language = Locale("en", "IN")
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            override fun onError(utteranceId: String?) {}
        })
    }

    fun speak(text: String) {
        if (!isReady) return
        
        // Stop any current speaking first
        tts.stop()
        
        val matchedHindi = hindiTranslations[text.trim()]
        nextHindiText = matchedHindi
        
        try {
            tts.language = Locale("en", "IN")
            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "english_speech")
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "english_speech")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        nextHindiText = null
        tts.stop()
    }

    fun shutdown() {
        nextHindiText = null
        tts.stop()
        tts.shutdown()
    }
}
