package com.gymbro.core.repository

import com.gymbro.core.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

interface WorkoutTemplateRepository {
    fun observeAllTemplates(): Flow<List<WorkoutTemplate>>
    suspend fun getAllTemplates(): List<WorkoutTemplate>
    suspend fun getTemplate(templateId: String): WorkoutTemplate?
    suspend fun saveTemplate(template: WorkoutTemplate)
    suspend fun deleteTemplate(templateId: String)
    suspend fun updateLastUsed(templateId: String)
    suspend fun initializeBuiltInTemplates()
}
