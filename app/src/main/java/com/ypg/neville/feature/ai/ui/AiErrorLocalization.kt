package com.ypg.neville.feature.ai.ui

import android.content.Context
import com.ypg.neville.R
import com.ypg.neville.feature.ai.network.OpenRouterErrorKind
import com.ypg.neville.feature.ai.network.OpenRouterException
import kotlin.math.ceil

fun Throwable.localizedAiMessage(context: Context): String {
    val error = this as? OpenRouterException
        ?: return message ?: context.getString(R.string.ai_error_operation)

    return when (error.kind) {
        OpenRouterErrorKind.MISSING_API_KEY -> context.getString(R.string.ai_error_missing_key)
        OpenRouterErrorKind.INVALID_API_KEY -> context.getString(R.string.ai_error_invalid_key)
        OpenRouterErrorKind.INSUFFICIENT_CREDITS -> context.getString(R.string.ai_error_credits)
        OpenRouterErrorKind.PERMISSION_DENIED -> context.getString(R.string.ai_error_permission)
        OpenRouterErrorKind.RATE_LIMITED -> error.retryAfterSeconds?.let {
            context.getString(R.string.ai_error_rate_seconds, ceil(it).toInt())
        } ?: context.getString(R.string.ai_error_rate)
        OpenRouterErrorKind.INVALID_REQUEST -> context.getString(R.string.ai_error_invalid_request)
        OpenRouterErrorKind.PAID_MODEL_NOT_ALLOWED -> context.getString(R.string.ai_error_paid_model)
        OpenRouterErrorKind.MODEL_UNAVAILABLE -> context.getString(R.string.ai_error_model_unavailable)
        OpenRouterErrorKind.SERVICE_UNAVAILABLE -> context.getString(R.string.ai_error_service_unavailable)
        OpenRouterErrorKind.OFFLINE -> context.getString(R.string.ai_error_offline)
        OpenRouterErrorKind.TIMEOUT -> context.getString(R.string.ai_error_timeout)
        OpenRouterErrorKind.NETWORK -> context.getString(R.string.ai_error_network)
        OpenRouterErrorKind.EMPTY_RESPONSE -> context.getString(R.string.ai_error_empty_response)
        OpenRouterErrorKind.RESPONSE_TRUNCATED -> context.getString(R.string.ai_error_truncated)
        OpenRouterErrorKind.CONTENT_FILTERED -> context.getString(R.string.ai_error_filtered)
        OpenRouterErrorKind.HTTP_STATUS -> context.getString(
            R.string.ai_error_http,
            error.statusCode?.toString() ?: "?"
        )
        OpenRouterErrorKind.API -> error.apiMessage?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.ai_error_api)
    }
}
