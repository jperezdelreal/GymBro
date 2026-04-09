package com.gymbro.core.error

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteDiskIOException
import java.io.IOException
import java.net.SocketException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

sealed class AppError(val message: String, val retryable: Boolean = false) {
    data class NetworkError(val cause: String) : AppError(
        message = "Network connection failed: $cause",
        retryable = true
    )
    
    data class DatabaseError(val cause: String, val isRetryable: Boolean = false) : AppError(
        message = "Database error: $cause",
        retryable = isRetryable
    )
    
    data class ValidationError(val cause: String) : AppError(
        message = "Invalid input: $cause",
        retryable = false
    )
    
    data class AuthError(val cause: String) : AppError(
        message = "Authentication failed: $cause",
        retryable = true
    )
    
    data class Unknown(val cause: String) : AppError(
        message = "An unexpected error occurred: $cause",
        retryable = true
    )
}

fun Throwable.toAppError(): AppError = when (this) {
    is UnknownHostException -> AppError.NetworkError("No internet connection")
    is SocketException -> AppError.NetworkError("Connection lost")
    is TimeoutException -> AppError.NetworkError("Request timed out")
    is SSLException -> AppError.NetworkError("Secure connection failed")
    is IOException -> AppError.NetworkError("Network error")
    is SQLiteDatabaseLockedException -> AppError.DatabaseError("Database is locked", isRetryable = true)
    is SQLiteDiskIOException -> AppError.DatabaseError("Database disk I/O error", isRetryable = true)
    is SQLiteException -> AppError.DatabaseError(this.message ?: "Database error", isRetryable = false)
    is IllegalArgumentException -> AppError.ValidationError(this.message ?: "Invalid argument")
    is IllegalStateException -> AppError.ValidationError(this.message ?: "Invalid state")
    else -> {
        val message = this.message ?: this.javaClass.simpleName
        when {
            message.contains("auth", ignoreCase = true) -> AppError.AuthError(message)
            message.contains("database", ignoreCase = true) -> AppError.DatabaseError(message)
            else -> AppError.Unknown(message)
        }
    }
}

fun Throwable.toUserMessage(): String = this.toAppError().message
