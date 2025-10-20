package com.devlabs.devlabsbackend.individualscore.domain.dto

import java.time.Instant
import java.util.*

data class SaveEvaluationDraftRequest(
    val reviewId: UUID,
    val projectId: UUID,
    val courseId: UUID,
    val scores: List<ParticipantScoreData>
)

data class EvaluationDraftResponse(
    val exists: Boolean,
    val draft: EvaluationDraft?
)

data class EvaluationDraft(
    val reviewId: UUID,
    val projectId: UUID,
    val courseId: UUID,
    val evaluatorId: String,
    val scores: List<ParticipantScoreData>,
    val lastUpdated: Instant,
    val isSubmitted: Boolean = false
)

data class SaveDraftSuccessResponse(
    val success: Boolean,
    val savedAt: Instant,
    val message: String
)

data class ClearDraftResponse(
    val success: Boolean,
    val message: String
)
