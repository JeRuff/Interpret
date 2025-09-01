package com.example.interpret.service

import android.util.Log
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechSynthesisOutputFormat
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer
import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageConfig
import com.microsoft.cognitiveservices.speech.AutoDetectSourceLanguageResult

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.util.concurrent.ConcurrentHashMap


class AzureSpeechService @Inject constructor(
    private val speechKey: String,
    private val speechRegion: String
) {
    private var recognizer: TranslationRecognizer? = null
    private var lastTranslatedText: String = "" // Track the last translated text to compute diffs
    private var lastTranslation: String = "" // Track the last translation to compute diffs
    private val TAG = "AzureSpeechService"
    private val processedEventIds = ConcurrentHashMap<String, Boolean>()


    suspend fun startContinuousTranslation(
        leftEarbudLanguage: String,
        rightEarbudLanguage: String,
        earbudLeft: String, // "left" or "right" for leftEarbudLanguage
        earbudRight: String, // "left" or "right" for rightEarbudLanguage
        onAudioOutput: (String, ByteArray) -> Unit) = withContext(Dispatchers.IO)
    {
            lastTranslatedText = "" // Reset last translated text
            val languages: List<String?>? = listOf(leftEarbudLanguage, rightEarbudLanguage)
            val autoDetectConfig = AutoDetectSourceLanguageConfig.fromLanguages(languages)

        val speechConfig = SpeechTranslationConfig.fromSubscription(speechKey, speechRegion).apply {
            addTargetLanguage(leftEarbudLanguage)
            addTargetLanguage(rightEarbudLanguage)
            addTargetLanguage("en-US") // Always include English for better detection

            voiceName = when (leftEarbudLanguage) {
                "fr-FR" -> "fr-FR-DeniseNeural"
                "lt-LT" -> "lt-LT-OnaNeural"
                "en-US" -> "en-US-JennyNeural"
                else -> "en-US-JennyNeural" // Fallback
                } // Faster utterance detection for real-time
                setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "3000")
            }

        val audioConfig = AudioConfig.fromDefaultMicrophoneInput()
        recognizer = TranslationRecognizer(speechConfig, autoDetectConfig, audioConfig)

        //COMMENTED OUT: Recognizing event listener - This will be useful if we want to display STT in real-time
         /*recognizer?.recognizing?.addEventListener { _, event ->
            if (event.result.reason == ResultReason.TranslatingSpeech) {
                val originalText = event.result.text ?: ""
                val newText = getNewText(originalText) // Compute only the new appended part
                // Translate only if enough info (3+ words)
                if (newText.split(" ").filter { it.isNotBlank() }.size >= 3) {
                    val translations = event.result.translations
                    Log.d(TAG, "Received translations: ${translations.keys}")
                    translations.forEach { (lang, lastTranslation) ->
                        val earbud = if(lang == leftEarbudLanguage) earbudLeft else earbudRight
                        synthesizeSpeech(getNewTranslation(lastTranslation), lang, earbud,onAudioOutput)
                    }
                    Log.d("Interpret Service", "translations :  $translations");
                    lastTranslatedText += newText // Update after processing
                }else{
                    // Log or handle cases where the text is too short
                    Log.d("Interpret Service", "Text too short for translation: $newText");
                    }
                Log.d("Interpret Service", "originalText :  $originalText");
                Log.d("Interpret Service", "newText: $newText");
                Log.d("Interpret Service", "lastTranslatedText: $lastTranslatedText");
            }
            else{
                Log.d("Interpret Service", "Recognizing event reason: ${event.result.reason}");
            }
            Log.d("Interpret Service", "Recognizing");
        }*/

/*        recognizer?.recognized?.addEventListener { _, event ->
            if (event.result.reason == ResultReason.TranslatedSpeech) {
                val originalText = event.result.text ?: ""
                val newText = getNewText(originalText)
                // Translate only if enough info (3+ words)
                if (newText.split(" ").filter { it.isNotBlank() }.size >= 3) {
                    val translations = event.result.translations
                    translations.forEach { (lang, lastTranslation) ->
                        val earbud = if(lang == leftEarbudLanguage) earbudLeft else earbudRight
                         // Compute only the new appended part
                        synthesizeSpeech(getNewTranslation((lastTranslation)), lang,earbud, onAudioOutput)
                        Log.d(TAG, "synthesizing speech for language: $lang, text: ${getNewTranslation(lastTranslation)}");
                    }
                    Log.d(TAG, "translated: $translations");
                }
                Log.d(TAG, "originalText: $originalText");
                lastTranslatedText += newText
                Log.d(TAG, "lastTranslatedText: $lastTranslatedText");
            }
            Log.d(TAG, "Recognized");
        }*/

        recognizer?.recognized?.addEventListener { _, event ->
            synchronized(this@AzureSpeechService) {
                val eventId = event.result.resultId
                val autoDetectResult = AutoDetectSourceLanguageResult.fromResult(event.result)
                val detectedLang = autoDetectResult?.language
                Log.d(TAG, "Detected language: $detectedLang for eventId=$eventId")

                val targetLang = if (detectedLang == leftEarbudLanguage) rightEarbudLanguage else leftEarbudLanguage
                Log.d(TAG, "Target language: $targetLang for eventId=$eventId")

                if (processedEventIds.containsKey(eventId)) {
                    Log.d(TAG, "Skipping duplicate event: resultId=$eventId")
                    return@addEventListener
                }
                processedEventIds[eventId] = true


                val translations = event.result.translations
                Log.d(TAG, "Received translations: ${translations.keys}")
                val earbud = if (targetLang == leftEarbudLanguage) "left" else "right"
                val text = event.result.translations[targetLang]

                if (text != null) {
                    Log.d(TAG, "Detected language=$detectedLang, translating to target=$targetLang, routing to earbud=$earbud, text='$text'")
                    synthesizeSpeech(text, targetLang, earbud, onAudioOutput)
                }
            }
        }

/*        recognizer?.recognized?.addEventListener { _, event ->
            val autoDetectResult = AutoDetectSourceLanguageResult.fromResult(event.result)
            val detectedLang = autoDetectResult?.language

            val targetLang = if (detectedLang == leftEarbudLanguage) rightEarbudLanguage else leftEarbudLanguage
            val earbud = if (targetLang == leftEarbudLanguage) "left" else "right"
            val text = event.result.translations[targetLang]
            if (text != null) {
                synthesizeSpeech(text, targetLang, earbud, onAudioOutput)
            }
        }*/

        recognizer?.startContinuousRecognitionAsync()?.get()
    }

    // Compute new text by removing previously recognized text
    private fun getNewText(currentText: String): String {
        //currentText.startsWith(lastTranslatedText) && lastTranslatedText.isNotEmpty()
        return if (currentText.regionMatches(0, lastTranslatedText, 0,lastTranslatedText.length)) {
            currentText.substring(lastTranslatedText.length)
        } else {
            currentText
        }
    }

    // Compute new Translation by removing previously translated text
    private fun getNewTranslation(currentTranslation: String): String {
        //currentTranslation.startsWith(lastTranslation) && lastTranslation.isNotEmpty()
        return if (currentTranslation.regionMatches(0, lastTranslation, 0,lastTranslation.length)) {
            currentTranslation.substring(lastTranslation.length)
        } else {
            currentTranslation
        }
    }

    private fun synthesizeSpeech(text: String, language: String,earbud:String, onAudioOutput: (String, ByteArray) -> Unit) {
        val speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion).apply {
            speechSynthesisVoiceName = when (language) {
                "fr-FR" -> "fr-FR-DeniseNeural"
                "lt-LT" -> "lt-LT-OnaNeural"
                "en-US" -> "en-US-JennyNeural"
                else -> {
                    Log.w(TAG, "Invalid language for synthesis: $language, using default")
                    "en-US-JennyNeural"
                }
            }
            setSpeechSynthesisOutputFormat(SpeechSynthesisOutputFormat.Raw24Khz16BitMonoPcm)
        }

        val synthesizer = com.microsoft.cognitiveservices.speech.SpeechSynthesizer(speechConfig, null)
        val result = synthesizer.SpeakTextAsync(text).get()
        if (result.reason == ResultReason.SynthesizingAudioCompleted) {
            Log.d(TAG, "Synthesis complete: audioData size=${result.audioData.size} bytes for earbud=$earbud")
            onAudioOutput(earbud, result.audioData) // This event is routed to the appropriate earbud
        }
        else {
            Log.e(TAG, "Synthesis failed for text='$text', reason=${result.reason}")
        }
        result.close()
        synthesizer.close()
    }

    fun stopTranslation() {
        recognizer?.stopContinuousRecognitionAsync()?.get()
        recognizer?.close()
        processedEventIds.clear()
        lastTranslatedText = ""; // Reset after synthesis to avoid re-sending same text
        Log.d(TAG, "Continuous recognition stopped")

    }
}