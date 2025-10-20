package com.devlabs.devlabsbackend.rubrics.domain.dto

import com.devlabs.devlabsbackend.criterion.domain.dto.CriterionResponse
import com.fasterxml.jackson.annotation.JsonTypeName
import java.io.Serializable
import java.sql.Timestamp
import java.util.*

interface RubricsListProjection {
    fun getId(): UUID?
    fun getName(): String
    fun getCreatedById(): String
    fun getCreatedByName(): String
    fun getCreatedByRole(): String
    fun getCreatedAt(): Timestamp
    fun getIsShared(): Boolean
}

data class CreateRubricsRequest(
    val name: String,
    val userId: String,
    val criteria: List<CreateCriterionRequest> = emptyList(),
    val isShared: Boolean = false
)

data class CreateCriterionRequest(
    val name: String,
    val description: String,
    val maxScore: Float,
    val isCommon: Boolean = false
)

data class UpdateRubricsRequest(
    val name: String,
    val userId: String,
    val criteria: List<CreateCriterionRequest> = emptyList(),
    val isShared: Boolean = false
)

@JsonTypeName("RubricsResponse")
data class RubricsResponse(
    val id: UUID?,
    val name: String,
    val createdBy: SimpleCreatedByInfo,
    val createdAt: Timestamp,
    val isShared: Boolean
) : Serializable

@JsonTypeName("RubricsDetailResponse")
data class RubricsDetailResponse(
    val id: UUID?,
    val name: String,
    val createdBy: SimpleCreatedByInfo,
    val createdAt: Timestamp,
    val isShared: Boolean,
    val criteria: List<CriterionResponse>
) : Serializable

@JsonTypeName("SimpleCreatedByInfo")
data class SimpleCreatedByInfo(
    val id: String,
    val name: String,
    val role: String
) : Serializable
