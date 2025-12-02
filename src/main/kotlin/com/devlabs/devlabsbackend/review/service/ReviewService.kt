package com.devlabs.devlabsbackend.review.service

import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.domain.dto.*
import org.springframework.stereotype.Service
import java.util.*

@Service
class ReviewService(
    private val reviewQueryService: ReviewQueryService,
    private val reviewCrudService: ReviewCrudService,
    private val reviewRelationshipService: ReviewRelationshipService
) {
    
    fun getReviewsForUser(
        userId: String,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "startDate",
        sortOrder: String = "desc"
    ): PaginatedResponse<ReviewResponse> =
        reviewQueryService.getReviewsForUser(userId, page, size, sortBy, sortOrder)

    fun getReviewById(reviewId: UUID, userId: String? = null): ReviewResponse =
        reviewQueryService.getReviewById(reviewId, userId)

    fun getUserBasedReview(reviewId: UUID, userId: String): ReviewResponse =
        reviewQueryService.getUserBasedReview(reviewId, userId)

    fun getReviewCriteria(reviewId: UUID): ReviewCriteriaResponse =
        reviewQueryService.getReviewCriteria(reviewId)

    fun searchReviews(
        userId: String,
        name: String?,
        courseId: UUID?,
        status: String?,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "startDate",
        sortOrder: String = "desc"
    ): PaginatedResponse<ReviewResponse> =
        reviewQueryService.searchReviews(userId, name, courseId, status, page, size, sortBy, sortOrder)

    fun checkProjectReviewAssignment(projectId: UUID): ReviewAssignmentResponse =
        reviewQueryService.checkProjectReviewAssignment(projectId)

    fun getReviewResults(reviewId: UUID, projectId: UUID, userId: String): ReviewResultsResponse =
        reviewQueryService.getReviewResults(reviewId, projectId, userId)
    
    fun getReviewProjectsWithFilters(
        reviewId: UUID,
        userId: String,
        teamId: UUID?,
        batchId: UUID?,
        courseId: UUID?
    ): ReviewProjectsResponse =
        reviewQueryService.getReviewProjectsWithFilters(reviewId, userId, teamId, batchId, courseId)

    fun getReviewExportData(
        reviewId: UUID,
        userId: String,
        batchIds: List<UUID>,
        courseIds: List<UUID>
    ): ReviewExportResponse =
        reviewQueryService.getReviewExportData(reviewId, userId, batchIds, courseIds)

    fun createReview(request: CreateReviewRequest): ReviewResponse =
        reviewCrudService.createReview(request)

    fun updateReview(reviewId: UUID, request: UpdateReviewRequest): ReviewResponse =
        reviewCrudService.updateReview(reviewId, request)

    fun deleteReview(reviewId: UUID, userId: String): Boolean =
        reviewCrudService.deleteReview(reviewId, userId)

    fun addFileToReview(reviewId: UUID, url: String) =
        reviewCrudService.addFileToReview(reviewId, url)

    fun removeFileFromReview(reviewId: UUID, url: String) =
        reviewCrudService.removeFileFromReview(reviewId, url)

    fun getPublicationStatus(reviewId: UUID): ReviewPublicationResponse =
        reviewRelationshipService.getPublicationStatus(reviewId)

    fun publishReview(reviewId: UUID, userId: String): ReviewPublicationResponse =
        reviewRelationshipService.publishReview(reviewId, userId)

    fun unpublishReview(reviewId: UUID, userId: String): ReviewPublicationResponse =
        reviewRelationshipService.unpublishReview(reviewId, userId)
}

fun Review.toReviewResponse(): ReviewResponse {
    return ReviewResponse(
        id = this.id,
        name = this.name,
        startDate = this.startDate,
        endDate = this.endDate,
        publishedAt = null,
        createdBy = this.createdBy?.let { creator ->
            CreatedByInfo(
                id = creator.id!!,
                name = creator.name,
                email = creator.email,
                role = creator.role.name
            )
        } ?: CreatedByInfo(
            id = "",
            name = "Unknown",
            email = "",
            role = "UNKNOWN"
        ),
        courses = this.courses.map {
            CourseInfo(
                id = it.id!!,
                name = it.name,
                code = it.code,
                semesterInfo = it.semester?.let { semester ->
                    SemesterInfo(
                        id = semester.id!!,
                        name = semester.name,
                        year = semester.year,
                        isActive = semester.isActive
                    )
                } ?: SemesterInfo(
                    id = UUID.randomUUID(),
                    name = "Unknown",
                    year = 0,
                    isActive = false
                )
            )
        },
        projects = this.projects.map {
            ProjectInfo(
                id = it.id!!,
                title = it.title,
                teamName = it.team.name,
                teamMembers = it.team.members.map { member ->
                    TeamMemberInfo(
                        id = member.id!!,
                        name = member.name
                    )
                }
            )
        },
        sections = this.batches.map { batch ->
            SectionInfo(
                id = batch.id!!,
                name = batch.name
            )
        },
        rubricsInfo = this.rubrics?.let {
            RubricInfo(
                id = it.id!!,
                name = it.name,
                criteria = it.criteria.map { criterion ->
                    CriteriaInfo(
                        id = criterion.id!!,
                        name = criterion.name,
                        description = criterion.description,
                        maxScore = criterion.maxScore,
                        isCommon = criterion.isCommon
                    )
                }
            )
        } ?: throw IllegalStateException("Review must have rubrics"),
        isPublished = null
    )
}
