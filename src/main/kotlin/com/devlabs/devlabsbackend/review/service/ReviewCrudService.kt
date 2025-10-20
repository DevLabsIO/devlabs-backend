package com.devlabs.devlabsbackend.review.service

import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.project.domain.ProjectStatus
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.domain.dto.CreateReviewRequest
import com.devlabs.devlabsbackend.review.domain.dto.ReviewResponse
import com.devlabs.devlabsbackend.review.domain.dto.UpdateReviewRequest
import com.devlabs.devlabsbackend.review.repository.ReviewRepository
import com.devlabs.devlabsbackend.rubrics.repository.RubricsRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class ReviewCrudService(
    private val reviewRepository: ReviewRepository,
    private val rubricsRepository: RubricsRepository,
    private val userRepository: UserRepository,
    private val reviewRelationshipService: ReviewRelationshipService
) {
    
    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["review-detail"], allEntries = true),
        CacheEvict(value = ["reviews"], allEntries = true),
        CacheEvict(value = [CacheConfig.REVIEW_DETAIL_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.REVIEWS_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT, CacheConfig.DASHBOARD_ADMIN], allEntries = true)
    ])
    fun createReview(request: CreateReviewRequest): ReviewResponse {
        val user = userRepository.findById(request.userId).orElseThrow {
            NotFoundException("User with id ${request.userId} not found")
        }

        if (user.role != Role.ADMIN && user.role != Role.MANAGER && user.role != Role.FACULTY) {
            throw ForbiddenException("Only admin, manager, or faculty can create reviews")
        }

        val rubrics = rubricsRepository.findById(request.rubricsId).orElseThrow {
            NotFoundException("Rubrics with id ${request.rubricsId} not found")
        }

        val review = Review(
            name = request.name,
            startDate = request.startDate,
            endDate = request.endDate,
            rubrics = rubrics,
            createdBy = user
        )

        val savedReview = reviewRepository.save(review)

        if (!request.courseIds.isNullOrEmpty()) {
            reviewRelationshipService.addCoursesToReview(savedReview, request.courseIds, user)
        }

        if (!request.semesterIds.isNullOrEmpty()) {
            reviewRelationshipService.addSemestersToReview(savedReview, request.semesterIds, user)
        }

        if (!request.projectIds.isNullOrEmpty()) {
            reviewRelationshipService.addProjectsToReview(savedReview, request.projectIds, user)
        }

        if (!request.batchIds.isNullOrEmpty()) {
            reviewRelationshipService.addBatchesToReview(savedReview, request.batchIds, user)
        }

        val finalReview = reviewRepository.save(savedReview)
        return finalReview.toReviewResponse()
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["review-detail"], allEntries = true),
        CacheEvict(value = ["reviews"], allEntries = true),
        CacheEvict(value = [CacheConfig.REVIEW_DETAIL_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.REVIEWS_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.INDIVIDUAL_SCORES_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
    ])
    fun updateReview(reviewId: UUID, request: UpdateReviewRequest): ReviewResponse {
        val user = userRepository.findById(request.userId).orElseThrow {
            NotFoundException("User with id ${request.userId} not found")
        }

        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }

        when (user.role) {
            Role.ADMIN, Role.MANAGER -> {}
            Role.FACULTY -> {
                if (review.createdBy?.id != user.id) {
                    throw ForbiddenException("You can only update reviews that you created")
                }
            }
            else -> {
                throw ForbiddenException("You don't have permission to update reviews")
            }
        }

        request.name?.let { review.name = it }
        request.startDate?.let { review.startDate = it }
        request.endDate?.let { review.endDate = it }

        if (request.rubricsId != null) {
            val rubrics = rubricsRepository.findById(request.rubricsId).orElseThrow {
                NotFoundException("Rubrics with id ${request.rubricsId} not found")
            }
            review.rubrics = rubrics
        }

        if (request.addCourseIds.isNotEmpty()) {
            reviewRelationshipService.addCoursesToReview(review, request.addCourseIds, user)
        }

        if (request.removeCourseIds.isNotEmpty()) {
            reviewRelationshipService.removeCoursesFromReview(review, request.removeCourseIds, user)
        }

        if (request.addSemesterIds.isNotEmpty()) {  
            reviewRelationshipService.addSemestersToReview(review, request.addSemesterIds, user)
        }

        if (request.removeSemesterIds.isNotEmpty()) {
            reviewRelationshipService.removeSemestersFromReview(review, request.removeSemesterIds, user)
        }

        if (request.addProjectIds.isNotEmpty()) {
            reviewRelationshipService.addProjectsToReview(review, request.addProjectIds, user)
        }

        if (request.removeProjectIds.isNotEmpty()) {
            reviewRelationshipService.removeProjectsFromReview(review, request.removeProjectIds, user)
        }

        if (request.addBatchIds.isNotEmpty()) {
            reviewRelationshipService.addBatchesToReview(review, request.addBatchIds, user)
        }

        if (request.removeBatchIds.isNotEmpty()) {
            reviewRelationshipService.removeBatchesFromReview(review, request.removeBatchIds, user)
        }

        val updatedReview = reviewRepository.save(review)
        return updatedReview.toReviewResponse()
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["review-detail"], allEntries = true),
        CacheEvict(value = ["reviews"], allEntries = true),
        CacheEvict(value = [CacheConfig.REVIEW_DETAIL_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.REVIEWS_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.INDIVIDUAL_SCORES_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT, CacheConfig.DASHBOARD_ADMIN], allEntries = true)
    ])
    fun deleteReview(reviewId: UUID, userId: String): Boolean {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }

        when (user.role) {
            Role.ADMIN, Role.MANAGER -> {}
            Role.FACULTY -> {
                if (review.createdBy?.id != user.id) {
                    throw ForbiddenException("You can only delete reviews that you created")
                }
            }
            else -> {
                throw ForbiddenException("You don't have permission to delete reviews")
            }
        }

        reviewRepository.delete(review)
        return true
    }

    @Transactional
    fun addFileToReview(reviewId: UUID, url: String) {
        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw IllegalArgumentException("Invalid URL format: $url")
        }

        review.files.add(url)
        reviewRepository.save(review)
    }

    @Transactional
    fun removeFileFromReview(reviewId: UUID, url: String) {
        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw IllegalArgumentException("Invalid URL format: $url")
        }

        if (!review.files.remove(url)) {
            throw IllegalArgumentException("File URL $url not found in review ${review.name}")
        }

        reviewRepository.save(review)
    }
}
