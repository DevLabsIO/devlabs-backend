package com.devlabs.devlabsbackend.review.controller

import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.review.domain.dto.CreateReviewRequest
import com.devlabs.devlabsbackend.review.domain.dto.ReviewPublicationResponse
import com.devlabs.devlabsbackend.review.domain.dto.ReviewResultsRequest
import com.devlabs.devlabsbackend.review.domain.dto.UpdateReviewRequest
import com.devlabs.devlabsbackend.review.service.ReviewService
import com.devlabs.devlabsbackend.security.utils.SecurityUtils
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/review")
class ReviewController(
    private val reviewService: ReviewService,
    private val userRepository: UserRepository
) {

    @PostMapping
    fun createReview(
        @RequestBody request: CreateReviewRequest
    ): ResponseEntity<Any> {
        return try {
            val review = reviewService.createReview(request)
            ResponseEntity.status(HttpStatus.CREATED).body(review)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to create review: ${e.message}"))
        }
    }

    @GetMapping("/{reviewId}")
    fun getReviewById(@PathVariable reviewId: UUID): ResponseEntity<Any> {
        return try {
            val userId = SecurityUtils.getCurrentUserId()
            val review = reviewService.getReviewById(reviewId, userId)
            ResponseEntity.ok(review)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get review: ${e.message}"))
        }
    }

    @PutMapping("/{reviewId}")
    fun updateReview(
        @PathVariable reviewId: UUID,
        @RequestBody request: UpdateReviewRequest
    ): ResponseEntity<Any> {
        return try {
            val updatedReview = reviewService.updateReview(reviewId, request)
            ResponseEntity.ok(updatedReview)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to update review: ${e.message}"))
        }
    }

    @DeleteMapping("/{reviewId}")
    fun deleteReview(
        @PathVariable reviewId: UUID,
        @RequestBody request: Map<String, String>
    ): ResponseEntity<Any> {
        return try {
            val userId = request["userId"] ?: throw IllegalArgumentException("userId is required")
            val result = reviewService.deleteReview(reviewId, userId)
            ResponseEntity.ok(mapOf("success" to result))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to delete review: ${e.message}"))
        }
    }

    @GetMapping
    fun getReviewsForUser(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortOrder: String?
    ): ResponseEntity<Any> {
        return try {
            val userId = SecurityUtils.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "User not authenticated"))
            
            val actualSortBy = if (sortBy.isNullOrBlank()) "startDate" else sortBy
            val actualSortOrder = if (sortOrder.isNullOrBlank()) "desc" else sortOrder
            
            val reviews = reviewService.getReviewsForUser(userId, page, size, actualSortBy, actualSortOrder)
            ResponseEntity.ok(reviews)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get reviews: ${e.message}"))
        }
    }

    @GetMapping("/user/{reviewId}")
    fun getUserBasedReview(@PathVariable reviewId: UUID): ResponseEntity<Any> {
        return try {
            val userId = SecurityUtils.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "User not authenticated"))
            
            val reviewResponse = reviewService.getUserBasedReview(reviewId, userId)
            ResponseEntity.ok(reviewResponse)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get user-based review: ${e.message}"))
        }
    }

    @GetMapping("/search")
    fun searchReviews(
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) courseId: UUID?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortOrder: String?
    ): ResponseEntity<Any> {
        return try {
            val userId = SecurityUtils.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "User not authenticated"))
            
            val actualSortBy = if (sortBy.isNullOrBlank()) "startDate" else sortBy
            val actualSortOrder = if (sortOrder.isNullOrBlank()) "desc" else sortOrder
            
            val reviews = reviewService.searchReviews(userId, name, courseId, status, page, size, actualSortBy, actualSortOrder)
            ResponseEntity.ok(reviews)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to search reviews: ${e.message}"))
        }
    }

    @GetMapping("/{reviewId}/publication")
    fun getPublicationStatus(
        @PathVariable reviewId: UUID,
        @RequestBody request: Map<String, String>
    ): ResponseEntity<Any> {
        return try {
            val userId = request["userId"] ?: throw IllegalArgumentException("userId is required")
            val user = userRepository.findById(userId).orElseThrow {
                NotFoundException("User with id $userId not found")
            }
            val publicationStatus = reviewService.getPublicationStatus(reviewId)

            val canPublish = when (user.role) {
                Role.ADMIN, Role.MANAGER, Role.FACULTY -> true
                else -> false
            }

            val responseWithPermission = ReviewPublicationResponse(
                reviewId = publicationStatus.reviewId,
                reviewName = publicationStatus.reviewName,
                isPublished = publicationStatus.isPublished,
                publishDate = publicationStatus.publishDate,
                canPublish = canPublish
            )

            ResponseEntity.ok(responseWithPermission)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get publication status: ${e.message}"))
        }
    }

    @PostMapping("/{reviewId}/publish")
    fun publishReview(
        @PathVariable reviewId: UUID,
        @RequestBody request: Map<String, String>
    ): ResponseEntity<Any> {
        return try {
            val userId = request["userId"] ?: throw IllegalArgumentException("userId is required")
            val publicationStatus = reviewService.publishReview(reviewId, userId)
            ResponseEntity.ok(publicationStatus)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to publish review: ${e.message}"))
        }
    }

    @PostMapping("/{reviewId}/unpublish")
    fun unpublishReview(
        @PathVariable reviewId: UUID,
        @RequestBody request: Map<String, String>
    ): ResponseEntity<Any> {
        return try {
            val userId = request["userId"] ?: throw IllegalArgumentException("userId is required")
            val publicationStatus = reviewService.unpublishReview(reviewId, userId)
            ResponseEntity.ok(publicationStatus)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to unpublish review: ${e.message}"))
        }
    }

    @GetMapping("/review/{reviewId}/criteria")
    fun getReviewCriteria(@PathVariable reviewId: UUID): ResponseEntity<Any> {
        return try {
            val criteria = reviewService.getReviewCriteria(reviewId)
            ResponseEntity.ok(criteria)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get review criteria: ${e.message}"))
        }
    }

    @GetMapping("/project/{projectId}/assignment")
    fun checkProjectReviewAssignment(@PathVariable projectId: UUID): ResponseEntity<Any> {
        return try {
            val assignment = reviewService.checkProjectReviewAssignment(projectId)
            ResponseEntity.ok(assignment)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to check project review assignment: ${e.message}"))
        }
    }

    @PostMapping("/{reviewId}/results")
    fun getReviewResults(
        @PathVariable reviewId: UUID,
        @RequestBody request: ReviewResultsRequest
    ): ResponseEntity<Any> {
        return try {
            val results = reviewService.getReviewResults(reviewId, request.projectId, request.userId)
            ResponseEntity.ok(results)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get review results: ${e.message}"))
        }
    }
    
    @GetMapping("/{reviewId}/projects")
    fun getReviewProjects(
        @PathVariable reviewId: UUID,
        @RequestParam(required = false) teamId: UUID?,
        @RequestParam(required = false) batchId: UUID?,
        @RequestParam(required = false) courseId: UUID?
    ): ResponseEntity<Any> {
        return try {
            val userId = SecurityUtils.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "User not authenticated"))
            
            val projects = reviewService.getReviewProjectsWithFilters(
                reviewId, userId, teamId, batchId, courseId
            )
            ResponseEntity.ok(projects)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get review projects: ${e.message}"))
        }
    }

    @GetMapping("/{reviewId}/export")
    fun getReviewExportData(
        @PathVariable reviewId: UUID,
        @RequestParam(required = false) batchIds: List<UUID>?,
        @RequestParam(required = false) courseIds: List<UUID>?
    ): ResponseEntity<Any> {
        return try {
            val userId = SecurityUtils.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "User not authenticated"))
            
            val exportData = reviewService.getReviewExportData(
                reviewId, userId, batchIds ?: emptyList(), courseIds ?: emptyList()
            )
            ResponseEntity.ok(exportData)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: ForbiddenException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get review export data: ${e.message}"))
        }
    }

    data class FileBodyRequest(
        val url: String
    )

    @PostMapping("/{reviewId}/add-file")
    fun addFileToReview(
        @PathVariable reviewId: UUID,
        @RequestBody request: FileBodyRequest
    ): ResponseEntity<Any> {
        return try {
            reviewService.addFileToReview(reviewId, request.url)
            ResponseEntity.ok(mapOf("success" to true))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to add file to review: ${e.message}"))
        }
    }

    @DeleteMapping("/{reviewId}/remove-file")
    fun removeFileFromReview(
        @PathVariable reviewId: UUID,
        @RequestBody request: FileBodyRequest
    ): ResponseEntity<Any> {
        return try {
            reviewService.removeFileFromReview(reviewId, request.url)
            ResponseEntity.ok(mapOf("success" to true))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to remove file from review: ${e.message}"))
        }
    }
}
