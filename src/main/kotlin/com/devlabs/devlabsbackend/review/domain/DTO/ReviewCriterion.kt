package com.devlabs.devlabsbackend.review.domain.DTO

import java.util.*

data class ReviewCriteriaResponse(
    val reviewId: UUID,
    val reviewName: String,
    val criteria: List<ReviewCriterionDetail>
)

data class ReviewCriterionDetail(
    val id: UUID,
    val name: String,
    val description: String,
    val maxScore: Float,
    val isCommon: Boolean
)
