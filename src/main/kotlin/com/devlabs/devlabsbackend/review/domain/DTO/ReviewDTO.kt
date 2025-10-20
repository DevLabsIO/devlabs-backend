package com.devlabs.devlabsbackend.review.domain.dto

import com.fasterxml.jackson.annotation.JsonTypeName
import java.io.Serializable
import java.time.LocalDate
import java.util.*

interface ReviewListProjection {
    fun getId(): UUID?
    fun getName(): String
    fun getStartDate(): LocalDate
    fun getEndDate(): LocalDate
    fun getCreatedById(): String?
    fun getCreatedByName(): String?
    fun getCreatedByRole(): String?
}

data class CreateReviewRequest(
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val rubricsId: UUID,
    val userId: String,
    val courseIds: List<UUID>? = null,
    val semesterIds: List<UUID>? = null,
    val batchIds: List<UUID>? = null,
    val projectIds: List<UUID>? = null,
    val sections: List<String>? = null
)

data class UpdateReviewRequest(
    val name: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val rubricsId: UUID? = null,
    val userId: String,
    val addCourseIds: List<UUID> = emptyList(),
    val removeCourseIds: List<UUID> = emptyList(),
    val addSemesterIds: List<UUID> = emptyList(),
    val removeSemesterIds: List<UUID> = emptyList(),
    val addBatchIds: List<UUID> = emptyList(),
    val removeBatchIds: List<UUID> = emptyList(),
    val addProjectIds: List<UUID> = emptyList(),
    val removeProjectIds: List<UUID> = emptyList(),
    val addSectionIds: List<UUID> = emptyList(),
    val removeSectionIds: List<UUID> = emptyList()
)

@JsonTypeName("ReviewResponse")
data class ReviewResponse(
    val id: UUID?,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val publishedAt: LocalDate?,
    val createdBy: CreatedByInfo,
    val courses: List<CourseInfo>,
    val projects: List<ProjectInfo>,
    val sections: List<String>,
    val rubricsInfo: RubricInfo,
    val isPublished: Boolean? = null
) : Serializable

@JsonTypeName("CreatedByInfo")
data class CreatedByInfo(
    val id: String,
    val name: String,
    val email: String,
    val role: String
) : Serializable

@JsonTypeName("ProjectInfo")
data class ProjectInfo(
    val id: UUID,
    val title: String,
    val teamName: String,
    val teamMembers: List<TeamMemberInfo>
) : Serializable

@JsonTypeName("TeamMemberInfo")
data class TeamMemberInfo(
    val id: String,
    val name: String
) : Serializable

@JsonTypeName("RubricInfo")
data class RubricInfo(
    val id: UUID,
    val name: String,
    val criteria: List<CriteriaInfo>
) : Serializable

@JsonTypeName("CriteriaInfo")
data class CriteriaInfo(
    val id: UUID,
    val name: String,
    val description: String,
    val maxScore: Float,
    val isCommon: Boolean
) : Serializable

@JsonTypeName("CourseInfo")
data class CourseInfo(
    val id: UUID,
    val name: String,
    val code: String,
    val semesterInfo: SemesterInfo
) : Serializable

@JsonTypeName("SemesterInfo")
data class SemesterInfo(
    val id: UUID,
    val name: String,
    val year: Int,
    val isActive: Boolean
) : Serializable

@JsonTypeName("ReviewCriteriaResponse")
data class ReviewCriteriaResponse(
    val reviewId: UUID,
    val reviewName: String,
    val criteria: List<ReviewCriterionDetail>
) : Serializable

@JsonTypeName("ReviewCriterionDetail")
data class ReviewCriterionDetail(
    val id: UUID,
    val name: String,
    val description: String,
    val maxScore: Float,
    val isCommon: Boolean
) : Serializable

data class PublishReviewRequest(
    val userId: String,
    val courseIds: List<UUID>
)

data class UnpublishReviewRequest(
    val userId: String,
    val courseIds: List<UUID>
)

@JsonTypeName("ReviewPublicationResponse")
data class ReviewPublicationResponse(
    val reviewId: UUID,
    val reviewName: String,
    val isPublished: Boolean,
    val publishDate: LocalDate? = null,
    val canPublish: Boolean = false
) : Serializable

data class ReviewAssignmentResponse(
    val hasReview: Boolean,
    val assignmentType: String,
    val liveReviews: List<ReviewResponse>,
    val upcomingReviews: List<ReviewResponse> = emptyList(),
    val completedReviews: List<ReviewResponse>
)

data class ReviewResultsResponse(
    val id: UUID,
    val title: String,
    val projectTitle: String,
    val reviewName: String,
    val isPublished: Boolean,
    val canViewAllResults: Boolean,
    val results: List<StudentResult>
)

data class StudentResult(
    val id: String,
    val name: String,
    val studentId: String,
    val studentName: String,
    val individualScore: Double,
    val totalScore: Double,
    val maxPossibleScore: Double,
    val percentage: Double,
    val scores: List<CriterionResult>
)

data class CriterionResult(
    val criterionId: UUID,
    val criterionName: String,
    val score: Double,
    val maxScore: Double,
    val comment: String?
)

data class ReviewResultsRequest(
    val userId: String,
    val projectId: UUID
)

fun ReviewListDTO.toReviewResponse(): ReviewResponse {
    return ReviewResponse(
        id = this.id,
        name = this.name,
        startDate = this.startDate,
        endDate = this.endDate,
        publishedAt = null,
        createdBy = CreatedByInfo(
            id = this.createdById ?: "",
            name = this.createdByName ?: "Unknown",
            email = this.createdByEmail ?: "",
            role = this.createdByRole?.name ?: "UNKNOWN"
        ),
        courses = emptyList(),
        projects = emptyList(),
        sections = emptyList(),
        rubricsInfo = RubricInfo(
            id = this.rubricsId ?: UUID.randomUUID(),
            name = this.rubricsName ?: "Unknown",
            criteria = emptyList()
        ),
        isPublished = this.publishedCourseCount > 0
    )
}
