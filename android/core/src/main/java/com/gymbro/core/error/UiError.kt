package com.gymbro.core.error

data class UiError(
    val message: String,
    val retryable: Boolean = false,
    val action: (() -> Unit)? = null
) {
    companion object {
        fun from(appError: AppError, retryAction: (() -> Unit)? = null): UiError {
            return UiError(
                message = appError.message,
                retryable = appError.retryable,
                action = if (appError.retryable) retryAction else null
            )
        }
        
        fun from(throwable: Throwable, retryAction: (() -> Unit)? = null): UiError {
            return from(throwable.toAppError(), retryAction)
        }
    }
}
