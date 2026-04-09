package com.gymbro.core.error

import kotlinx.coroutines.delay

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val error: AppError) : AppResult<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw Exception(error.message)
    }
}

inline fun <T> AppResult<T>.getOrDefault(default: T): T = when (this) {
    is AppResult.Success -> data
    is AppResult.Error -> default
}

inline fun <T> runCatchingAsResult(block: () -> T): AppResult<T> {
    return try {
        AppResult.Success(block())
    } catch (e: Exception) {
        AppResult.Error(e.toAppError())
    }
}

suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelayMs: Long = 100,
    maxDelayMs: Long = 2000,
    factor: Double = 2.0,
    retryableErrors: Set<Class<out Throwable>> = setOf(
        android.database.sqlite.SQLiteDatabaseLockedException::class.java,
        android.database.sqlite.SQLiteDiskIOException::class.java,
        java.io.IOException::class.java,
    ),
    block: suspend () -> AppResult<T>
): AppResult<T> {
    var currentDelay = initialDelayMs
    var lastError: AppError? = null

    repeat(maxRetries) { attempt ->
        val result = block()
        when (result) {
            is AppResult.Success -> return result
            is AppResult.Error -> {
                lastError = result.error
                if (!result.error.retryable) {
                    return result
                }
                if (attempt < maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
                }
            }
        }
    }

    return AppResult.Error(lastError ?: AppError.Unknown("Retry failed"))
}
