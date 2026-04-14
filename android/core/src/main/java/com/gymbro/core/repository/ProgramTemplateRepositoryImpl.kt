package com.gymbro.core.repository

import android.content.Context
import android.util.Log
import com.gymbro.core.error.AppResult
import com.gymbro.core.error.retryWithBackoff
import com.gymbro.core.error.runCatchingAsResult
import com.gymbro.core.model.ExperienceLevel
import com.gymbro.core.model.ProgramTemplate
import com.gymbro.core.model.TrainingGoal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramTemplateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ProgramTemplateRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val _templates = MutableStateFlow<List<ProgramTemplate>>(emptyList())
    private var isInitialized = false

    private val isSpanish: Boolean
        get() = context.resources.configuration.locales[0].language == "es"

    override fun observeAllTemplates(): Flow<List<ProgramTemplate>> {
        return _templates.asStateFlow()
    }

    override suspend fun getAllTemplates(): List<ProgramTemplate> {
        if (!isInitialized) {
            loadTemplatesFromAssets()
        }
        return _templates.value
    }

    override suspend fun getTemplate(templateId: String): ProgramTemplate? {
        if (!isInitialized) {
            loadTemplatesFromAssets()
        }
        return _templates.value.find { it.id.toString() == templateId }
    }

    override suspend fun filterTemplates(
        goal: TrainingGoal?,
        experienceLevel: ExperienceLevel?,
        frequencyPerWeek: Int?,
    ): List<ProgramTemplate> {
        if (!isInitialized) {
            loadTemplatesFromAssets()
        }
        return _templates.value.filter { template ->
            (goal == null || template.primaryGoal == goal) &&
                (experienceLevel == null || template.targetAudience == experienceLevel) &&
                (frequencyPerWeek == null || template.frequencyPerWeek == frequencyPerWeek)
        }
    }

    private suspend fun loadTemplatesFromAssets() {
        val useSpanish = isSpanish
        val result = retryWithBackoff {
            runCatchingAsResult {
                val jsonString = context.assets.open("programs-seed.json").bufferedReader().use { it.readText() }
                val dtos = json.decodeFromString<List<ProgramTemplateDto>>(jsonString)
                dtos.map { it.toDomain(useSpanish) }
            }
        }
        when (result) {
            is AppResult.Success -> {
                _templates.value = result.data
                isInitialized = true
                Log.d(TAG, "Loaded ${result.data.size} program templates from assets")
            }
            is AppResult.Error -> {
                Log.e(TAG, "Failed to load program templates: ${result.error.message}")
                _templates.value = emptyList()
            }
        }
    }

    companion object {
        private const val TAG = "ProgramTemplateRepositoryImpl"
    }
}
