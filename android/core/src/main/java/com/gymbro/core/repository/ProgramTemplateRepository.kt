package com.gymbro.core.repository

import com.gymbro.core.model.ExperienceLevel
import com.gymbro.core.model.ProgramTemplate
import com.gymbro.core.model.TrainingGoal
import kotlinx.coroutines.flow.Flow

interface ProgramTemplateRepository {
    fun observeAllTemplates(): Flow<List<ProgramTemplate>>
    suspend fun getAllTemplates(): List<ProgramTemplate>
    suspend fun getTemplate(templateId: String): ProgramTemplate?
    suspend fun filterTemplates(
        goal: TrainingGoal? = null,
        experienceLevel: ExperienceLevel? = null,
        frequencyPerWeek: Int? = null,
    ): List<ProgramTemplate>
}
