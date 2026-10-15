package com.ypg.neville.feature.translation.data

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.ypg.neville.feature.translation.domain.BatchTranslationResult
import com.ypg.neville.feature.translation.domain.OnDeviceTranslationService
import com.ypg.neville.feature.translation.domain.TranslationLanguage
import com.ypg.neville.feature.translation.domain.TranslationResult
import com.ypg.neville.feature.translation.domain.UnsupportedSourceLanguageException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * ML Kit implementation shared by the system-share importer and future consumers such as Notes.
 * Language identification and translation happen locally. Translation models are downloaded by
 * ML Kit on first use and are then available offline.
 */
class MlKitOnDeviceTranslationService(
    private val languageIdentifier: LanguageIdentifier = LanguageIdentification.getClient(),
    private val downloadConditions: DownloadConditions = DownloadConditions.Builder()
        .requireWifi()
        .build()
) : OnDeviceTranslationService {

    override suspend fun identifyLanguage(text: String): TranslationLanguage {
        val detectedTag = languageIdentifier.identifyLanguage(text.requireContent()).awaitResult()
        return TranslationLanguage.fromLanguageTag(detectedTag)
            ?: throw UnsupportedSourceLanguageException(detectedTag.takeUnless { it == UNDETERMINED })
    }

    override suspend fun translate(
        text: String,
        targetLanguage: TranslationLanguage,
        sourceLanguage: TranslationLanguage?
    ): TranslationResult {
        val result = translateBatch(
            texts = listOf(text),
            targetLanguage = targetLanguage,
            sourceLanguage = sourceLanguage
        )
        return TranslationResult(
            text = result.texts.single(),
            sourceLanguage = result.sourceLanguage,
            targetLanguage = result.targetLanguage,
            wasTranslated = result.wasTranslated
        )
    }

    override suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: TranslationLanguage,
        sourceLanguage: TranslationLanguage?
    ): BatchTranslationResult {
        require(texts.isNotEmpty()) { "At least one text is required" }
        val normalizedTexts = texts.map(String::trim)
        val detectionText = normalizedTexts.filter(String::isNotEmpty).joinToString("\n")
        val resolvedSource = sourceLanguage ?: identifyLanguage(detectionText)

        if (resolvedSource == targetLanguage) {
            return BatchTranslationResult(
                texts = normalizedTexts,
                sourceLanguage = resolvedSource,
                targetLanguage = targetLanguage,
                wasTranslated = false
            )
        }

        val sourceTag = requireNotNull(
            TranslateLanguage.fromLanguageTag(resolvedSource.mlKitLanguageTag)
        )
        val targetTag = requireNotNull(
            TranslateLanguage.fromLanguageTag(targetLanguage.mlKitLanguageTag)
        )
        val translator = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceTag)
                .setTargetLanguage(targetTag)
                .build()
        )

        return try {
            translator.downloadModelIfNeeded(downloadConditions).awaitResult()
            BatchTranslationResult(
                texts = normalizedTexts.map { content ->
                    if (content.isEmpty()) content else translator.translate(content).awaitResult()
                },
                sourceLanguage = resolvedSource,
                targetLanguage = targetLanguage,
                wasTranslated = true
            )
        } finally {
            translator.close()
        }
    }

    override fun close() {
        languageIdentifier.close()
    }

    private fun String.requireContent(): String = trim().also {
        require(it.isNotEmpty()) { "Text to translate cannot be blank" }
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCoroutine { continuation ->
        addOnSuccessListener(continuation::resume)
        addOnFailureListener(continuation::resumeWithException)
        addOnCanceledListener {
            continuation.resumeWithException(
                java.util.concurrent.CancellationException("ML Kit task was cancelled")
            )
        }
    }

    private companion object {
        const val UNDETERMINED = "und"
    }
}
