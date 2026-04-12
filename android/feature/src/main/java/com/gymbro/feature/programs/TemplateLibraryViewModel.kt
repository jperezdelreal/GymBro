package com.gymbro.feature.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymbro.core.model.WorkoutTemplateLibrary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateLibraryViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(TemplateLibraryState())
    val state: StateFlow<TemplateLibraryState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<TemplateLibraryEffect>()
    val effect: SharedFlow<TemplateLibraryEffect> = _effect.asSharedFlow()

    fun onEvent(event: TemplateLibraryEvent) {
        when (event) {
            is TemplateLibraryEvent.FilterChanged -> {
                _state.update { state ->
                    val filteredTemplates = if (event.audience == null) {
                        state.templates
                    } else {
                        state.templates.filter { it.targetAudience == event.audience }
                    }
                    state.copy(
                        selectedFilter = event.audience,
                        filteredTemplates = filteredTemplates
                    )
                }
            }
            is TemplateLibraryEvent.TemplateClicked -> {
                viewModelScope.launch {
                    _effect.emit(TemplateLibraryEffect.NavigateToTemplateDetail(event.template))
                }
            }
        }
    }
}
