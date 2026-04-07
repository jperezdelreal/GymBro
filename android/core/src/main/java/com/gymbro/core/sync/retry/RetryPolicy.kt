package com.gymbro.core.sync.retry

import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import kotlin.math.pow

data class RetryConfig(
    val maxRetries: Int = 3,
    val initialDelayMs: Long = 1000L,
    val maxDelayMs: Long = 8000L,
    val backoffMultiplier: Double = 2.0
)

sealed class RetryableError {
    data class Retryable(val cause: Throwable) : RetryableError()
    data class Permanent(val cause: Throwable) : RetryableError()
}

fun Throwable.classifyError(): RetryableError {
    return when (this) {
        is UnknownHostException,
        is SocketException,
        is TimeoutException,
        is IOException,
        is FirebaseNetworkException -> RetryableError.Retryable(this)
        
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED,
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED,
            FirebaseFirestoreException.Code.ABORTED,
            FirebaseFirestoreException.Code.INTERNAL,
            FirebaseFirestoreException.Code.UNKNOWN -> RetryableError.Retryable(this)
            
            FirebaseFirestoreException.Code.UNAUTHENTICATED,
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.INVALID_ARGUMENT,
            FirebaseFirestoreException.Code.NOT_FOUND,
            FirebaseFirestoreException.Code.ALREADY_EXISTS,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION -> RetryableError.Permanent(this)
            
            else -> RetryableError.Permanent(this)
        }
        
        is FirebaseAuthException -> when (errorCode) {
            "ERROR_NETWORK_REQUEST_FAILED" -> RetryableError.Retryable(this)
            "ERROR_USER_TOKEN_EXPIRED",
            "ERROR_INVALID_CUSTOM_TOKEN",
            "ERROR_CUSTOM_TOKEN_MISMATCH",
            "ERROR_INVALID_CREDENTIAL" -> RetryableError.Permanent(this)
            else -> RetryableError.Permanent(this)
        }
        
        is FirebaseException -> {
            if (message?.contains("network", ignoreCase = true) == true ||
                message?.contains("timeout", ignoreCase = true) == true) {
                RetryableError.Retryable(this)
            } else {
                RetryableError.Permanent(this)
            }
        }
        
        else -> {
            val msg = message ?: ""
            when {
                msg.contains("network", ignoreCase = true) ||
                msg.contains("timeout", ignoreCase = true) ||
                msg.contains("connection", ignoreCase = true) -> RetryableError.Retryable(this)
                msg.contains("auth", ignoreCase = true) ||
                msg.contains("permission", ignoreCase = true) ||
                msg.contains("invalid", ignoreCase = true) -> RetryableError.Permanent(this)
                else -> RetryableError.Retryable(this)
            }
        }
    }
}

suspend fun <T> retryWithBackoff(
    config: RetryConfig = RetryConfig(),
    operation: String = "operation",
    block: suspend () -> T
): Result<T> {
    var lastException: Throwable? = null
    
    repeat(config.maxRetries + 1) { attempt ->
        try {
            return Result.success(block())
        } catch (e: Exception) {
            lastException = e
            
            val errorType = e.classifyError()
            if (errorType is RetryableError.Permanent) {
                Log.w(TAG, "Permanent error for $operation, not retrying: ${e.message}")
                return Result.failure(e)
            }
            
            if (attempt < config.maxRetries) {
                val delayMs = (config.initialDelayMs * config.backoffMultiplier.pow(attempt))
                    .toLong()
                    .coerceAtMost(config.maxDelayMs)
                
                Log.w(TAG, "Retry $operation: attempt ${attempt + 1}/${config.maxRetries + 1} failed, " +
                        "retrying in ${delayMs}ms: ${e.message}")
                delay(delayMs)
            } else {
                Log.e(TAG, "Retry $operation: all ${config.maxRetries + 1} attempts failed", e)
            }
        }
    }
    
    return Result.failure(lastException ?: Exception("Unknown error"))
}

private const val TAG = "RetryPolicy"
