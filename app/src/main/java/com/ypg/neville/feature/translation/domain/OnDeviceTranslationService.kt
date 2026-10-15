package com.ypg.neville.feature.translation.domain

/** Reusable contract for translations that never send the user's text off-device. */
interface OnDeviceTranslationService : AutoCloseable {

    suspend fun identifyLanguage(text: String): TranslationLanguage

    suspend fun translate(
        text: String,
        targetLanguage: TranslationLanguage,
        sourceLanguage: TranslationLanguage? = null
    ): TranslationResult

    suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: TranslationLanguage,
        sourceLanguage: TranslationLanguage? = null
    ): BatchTranslationResult
}

data class TranslationResult(
    val text: String,
    val sourceLanguage: TranslationLanguage,
    val targetLanguage: TranslationLanguage,
    val wasTranslated: Boolean
)

data class BatchTranslationResult(
    val texts: List<String>,
    val sourceLanguage: TranslationLanguage,
    val targetLanguage: TranslationLanguage,
    val wasTranslated: Boolean
)

class UnsupportedSourceLanguageException(
    val detectedLanguageTag: String?
) : Exception("The source text is not Spanish, English, or Simplified Chinese")
