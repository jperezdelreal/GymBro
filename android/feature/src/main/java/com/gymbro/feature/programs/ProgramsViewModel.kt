package com.gymbro.feature.programs

import androidx.lifecycle.viewModelScope
import com.gymbro.core.repository.WorkoutTemplateRepository
import com.gymbro.feature.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val templateRepository: WorkoutTemplateRepository,
) : BaseViewModel() {

    private val _state = MutableStateFlow(ProgramsState())
    val state: StateFlow<ProgramsState> = _state.asStateFlow()

    private val _effect = Channel<ProgramsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var loadJob: Job? = null

    init {
        initializeTemplates()
        loadTemplates()
    }

    fun onEvent(event: ProgramsEvent) {
        when (event) {
            is ProgramsEvent.TemplateClicked -> {
                // Show options dialog or navigate to detail/edit
                viewModelScope.launch {
                    _effect.send(ProgramsEffect.NavigateToCreateTemplate(event.template.id.toString()))
                }
            }
            is ProgramsEvent.CreateTemplateClicked -> {
                _state.update { it.copy(showCreateDialog = true) }
            }
            is ProgramsEvent.CreateDialogDismissed -> {
                _state.update { it.copy(showCreateDialog = false) }
            }
            is ProgramsEvent.DeleteTemplate -> {
                safeLaunch {
                    templateRepository.deleteTemplate(event.templateId)
                }
            }
            is ProgramsEvent.StartWorkoutFromTemplate -> {
                viewModelScope.launch {
                    templateRepository.updateLastUsed(event.template.id.toString())
                    _effect.send(ProgramsEffect.NavigateToActiveWorkout(event.template))
                }
            }
        }
    }

    private fun initializeTemplates() {
        safeLaunch {
            templateRepository.initializeBuiltInTemplates()
        }
    }

    private fun loadTemplates() {
        loadJob?.cancel()
        loadJob = safeLaunch(
            onError = { error ->
                _state.update { it.copy(isLoading = false) }
                handleError(error) { loadTemplates() }
            }
        ) {
            _state.update { it.copy(isLoading = true) }
            templateRepository.observeAllTemplates()
                .collect { templates ->
                    _state.update {
                        it.copy(templates = templates, isLoading = false, error = null)
                    }
                }
        }
    }

    override fun setLoading(loading: Boolean) {
        _state.update { it.copy(isLoading = loading) }
    }
}
