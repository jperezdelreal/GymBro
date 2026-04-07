package com.gymbro.feature.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.error.UiError
import com.gymbro.core.error.toAppError
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {
    
    private val _errorEvents = Channel<UiError>(Channel.BUFFERED)
    val errorEvents = _errorEvents.receiveAsFlow()
    
    protected fun handleError(throwable: Throwable, retryAction: (() -> Unit)? = null): UiError {
        val uiError = UiError.from(throwable, retryAction)
        viewModelScope.launch {
            _errorEvents.send(uiError)
        }
        return uiError
    }
    
    protected fun safeLaunch(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            onError?.invoke(throwable) ?: handleError(throwable)
        }
        
        return viewModelScope.launch(exceptionHandler) {
            try {
                block()
            } catch (e: Exception) {
                onError?.invoke(e) ?: handleError(e)
            }
        }
    }
    
    protected open fun setLoading(loading: Boolean) {
        // Override in subclass to update loading state
    }
}
