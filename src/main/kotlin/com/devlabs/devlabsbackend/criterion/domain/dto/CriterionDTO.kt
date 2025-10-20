package com.devlabs.devlabsbackend.criterion.domain.dto

import com.fasterxml.jackson.annotation.JsonTypeName
import java.io.Serializable
import java.util.*

@JsonTypeName("CriterionResponse")
data class CriterionResponse(
    val id: UUID?,
    val name: String,
    val description: String,
    val maxScore: Float,
    val isCommon: Boolean
) : Serializable

data class UserIdRequest(
    val userId: String
)
