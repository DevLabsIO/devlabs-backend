package com.devlabs.devlabsbackend.semester.domain.dto

import com.fasterxml.jackson.annotation.JsonTypeName
import java.io.Serializable
import java.util.*

interface SemesterListProjection {
    fun getId(): UUID?
    fun getName(): String
    fun getYear(): Int
    fun getIsActive(): Boolean
}

@JsonTypeName("SemesterResponse")
data class SemesterResponse(
    val id: UUID?,
    val name: String,
    val year: Int,
    val isActive: Boolean
) : Serializable

@JsonTypeName("SemesterDetailResponse")
data class SemesterDetailResponse(
    val id: UUID?,
    val name: String,
    val year: Int,
    val isActive: Boolean,
    val courses: List<SimpleCourseInfo>,
    val managers: List<SimpleUserInfo>,
    val batches: List<SimpleBatchInfo>
) : Serializable

data class SimpleCourseInfo(
    val id: UUID?,
    val name: String,
    val code: String,
    val credits: Int,
    val isActive: Boolean
) : Serializable

data class SimpleUserInfo(
    val id: String,
    val name: String,
    val email: String,
    val role: String
) : Serializable

data class SimpleBatchInfo(
    val id: UUID?,
    val name: String,
    val section: String
) : Serializable

data class CreateSemesterRequest(
    val name: String,
    val year: Int,
    val isActive: Boolean = true
)

data class UpdateSemesterRequest(
    val name: String? = null,
    val year: Int? = null,
    val isActive: Boolean? = null
)

data class AddManagersRequest(
    val managerIds: List<String>
)

data class RemoveManagersRequest(
    val managerIds: List<String>
)
