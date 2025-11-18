package com.knowbody.interpret.model

data class Language(
    val code: String,
    val displayName: String,
    val voiceName: String
)

object LanguageConfig {
    val availableLanguages = listOf(
        Language("fr-FR", "French (France)", "fr-FR-DeniseNeural"),
        Language("lt-LT", "Lithuanian (Lithuania)", "lt-LT-OnaNeural"),
        Language("en-US", "English (US)", "en-US-JennyNeural"),
    )

    fun getVoiceForLanguage(languageCode: String): String {
        return availableLanguages.find { it.code == languageCode }?.voiceName
            ?: "en-US-JennyNeural"
    }
}