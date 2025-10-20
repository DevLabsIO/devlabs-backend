package com.devlabs.devlabsbackend.individualscore.service

import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.individualscore.domain.dto.EvaluationDraft
import com.devlabs.devlabsbackend.individualscore.domain.dto.EvaluationDraftResponse
import com.devlabs.devlabsbackend.individualscore.domain.dto.ParticipantScoreData
import com.devlabs.devlabsbackend.individualscore.domain.dto.SaveEvaluationDraftRequest
import com.devlabs.devlabsbackend.individualscore.repository.IndividualScoreRepository
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.review.repository.ReviewRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class EvaluationDraftService(
    private val userRepository: UserRepository,
    private val reviewRepository: ReviewRepository,
    private val projectRepository: ProjectRepository,
    private val courseRepository: CourseRepository,
    private val individualScoreRepository: IndividualScoreRepository,
    private val cacheManager: CacheManager
) {

    @Transactional(readOnly = true)
    fun getDraft(reviewId: UUID, projectId: UUID, courseId: UUID, evaluatorId: String): EvaluationDraftResponse {
        val isSubmitted = checkIfEvaluationSubmitted(reviewId, projectId, courseId, evaluatorId)
        if (isSubmitted) {
            return EvaluationDraftResponse(exists = false, draft = null)
        }
        
        val cacheKey = "draft-$reviewId-$projectId-$courseId-$evaluatorId"
        val cache = cacheManager.getCache(CacheConfig.EVALUATION_DRAFTS_CACHE)
        val cachedValue = cache?.get(cacheKey, EvaluationDraftResponse::class.java)
        
        if (cachedValue != null) {
            return cachedValue
        }
        
        return EvaluationDraftResponse(exists = false, draft = null)
    }

    @Transactional
    fun saveDraft(request: SaveEvaluationDraftRequest, evaluatorId: String): EvaluationDraftResponse {
        val isSubmitted = checkIfEvaluationSubmitted(request.reviewId, request.projectId, request.courseId, evaluatorId)
        if (isSubmitted) {
            throw IllegalStateException("Cannot save draft for already submitted evaluation")
        }

        val draft = EvaluationDraft(
            reviewId = request.reviewId,
            projectId = request.projectId,
            courseId = request.courseId,
            evaluatorId = evaluatorId,
            scores = request.scores,
            lastUpdated = Instant.now(),
            isSubmitted = false
        )
        
        val response = EvaluationDraftResponse(exists = true, draft = draft)
        
        val cacheKey = "draft-${draft.reviewId}-${draft.projectId}-${draft.courseId}-${draft.evaluatorId}"
        val cache = cacheManager.getCache(CacheConfig.EVALUATION_DRAFTS_CACHE)
        cache?.put(cacheKey, response)
        
        return response
    }

    @Transactional
    @CacheEvict(
        value = [CacheConfig.EVALUATION_DRAFTS_CACHE],
        key = "'draft-' + #reviewId + '-' + #projectId + '-' + #courseId + '-' + #evaluatorId"
    )
    fun clearDraft(reviewId: UUID, projectId: UUID, courseId: UUID, evaluatorId: String) {
        validateAccess(reviewId, projectId, courseId, evaluatorId)
    }

    @Transactional
    @CacheEvict(
        value = [CacheConfig.EVALUATION_DRAFTS_CACHE],
        key = "'draft-' + #reviewId + '-' + #projectId + '-' + #courseId + '-' + #evaluatorId"
    )
    fun clearDraftOnSubmission(reviewId: UUID, projectId: UUID, courseId: UUID, evaluatorId: String) {
    }

    private fun validateAccess(reviewId: UUID, projectId: UUID, courseId: UUID, evaluatorId: String) {
        val evaluator = userRepository.findById(evaluatorId).orElseThrow {
            NotFoundException("User with id $evaluatorId not found")
        }

        reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }

        val project = projectRepository.findById(projectId).orElseThrow {
            NotFoundException("Project with id $projectId not found")
        }

        val course = courseRepository.findById(courseId).orElseThrow {
            NotFoundException("Course with id $courseId not found")
        }

        if (!project.courses.contains(course)) {
            throw IllegalArgumentException("Course is not associated with this project")
        }

        when (evaluator.role) {
            Role.ADMIN, Role.MANAGER -> return
            Role.FACULTY -> {
                if (!course.instructors.contains(evaluator)) {
                    throw ForbiddenException("Faculty can only access evaluations for courses they teach")
                }
            }
            else -> throw ForbiddenException("Only faculty, admin, or manager can access evaluation drafts")
        }
    }

    private fun checkIfEvaluationSubmitted(reviewId: UUID, projectId: UUID, courseId: UUID, evaluatorId: String): Boolean {
        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }
        val project = projectRepository.findById(projectId).orElseThrow {
            NotFoundException("Project with id $projectId not found")
        }
        val course = courseRepository.findById(courseId).orElseThrow {
            NotFoundException("Course with id $courseId not found")
        }

        val expectedScoreCount = project.team.members.size * (review.rubrics?.criteria?.size ?: 0)
        
        val existingScores = individualScoreRepository.findByReviewAndProjectAndCourse(review, project, course)
        
        return existingScores.size == expectedScoreCount && existingScores.isNotEmpty()
    }
}
