package com.devlabs.devlabsbackend.review.service

import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.project.domain.ProjectStatus
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.domain.ReviewCoursePublication
import com.devlabs.devlabsbackend.review.domain.dto.ReviewPublicationResponse
import com.devlabs.devlabsbackend.review.repository.ReviewCoursePublicationRepository
import com.devlabs.devlabsbackend.review.repository.ReviewRepository
import com.devlabs.devlabsbackend.semester.repository.SemesterRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class ReviewRelationshipService(
    private val reviewRepository: ReviewRepository,
    private val courseRepository: CourseRepository,
    private val projectRepository: ProjectRepository,
    private val batchRepository: BatchRepository,
    private val userRepository: UserRepository,
    private val reviewCoursePublicationRepository: ReviewCoursePublicationRepository,
    private val reviewPublicationHelper: ReviewPublicationHelper
) {
    
    private val logger = LoggerFactory.getLogger(ReviewRelationshipService::class.java)
    
    fun addCoursesToReview(review: Review, courseIds: List<UUID>, user: User) {
        val courses = courseRepository.findAllById(courseIds)

        val coursesToAdd = if (user.role == Role.FACULTY) {
            courses.filter { course -> course.instructors.contains(user) }
        } else {
            courses
        }

        coursesToAdd.forEach { course ->
            if (!review.courses.contains(course)) {
                review.courses.add(course)
            }
        }
    }

    fun removeCoursesFromReview(review: Review, courseIds: List<UUID>, user: User) {
        val courses = courseRepository.findAllById(courseIds)

        val coursesToRemove = if (user.role == Role.FACULTY) {
            courses.filter { course -> course.instructors.contains(user) }
        } else {
            courses
        }

        coursesToRemove.forEach { course ->
            review.courses.remove(course)
        }
    }

    fun addProjectsToReview(review: Review, projectIds: List<UUID>, user: User) {
        val projects = projectRepository.findAllById(projectIds)

        val validProjects = projects.filter {
            it.status == ProjectStatus.ONGOING || it.status == ProjectStatus.PROPOSED
        }

        if (validProjects.size != projects.size) {
            throw IllegalArgumentException("Some projects are not valid for review (must be Ongoing or Proposed)")
        }

        val projectsToAdd = if (user.role == Role.FACULTY) {
            validProjects.filter { project ->
                project.courses.any { course -> course.instructors.contains(user) }
            }
        } else {
            validProjects
        }

        projectsToAdd.forEach { project ->
            if (!review.projects.contains(project)) {
                review.projects.add(project)
            }
        }
    }

    fun removeProjectsFromReview(review: Review, projectIds: List<UUID>, user: User) {
        val projects = projectRepository.findAllById(projectIds)

        val projectsToRemove = if (user.role == Role.FACULTY) {
            projects.filter { project ->
                project.courses.any { course -> course.instructors.contains(user) }
            }
        } else {
            projects
        }

        projectsToRemove.forEach { project ->
            review.projects.remove(project)
        }
    }

    fun addBatchesToReview(review: Review, batchIds: List<UUID>, user: User) {
        val batches = batchRepository.findAllById(batchIds)

        if (user.role == Role.FACULTY) {
            val batchCourses = courseRepository.findCoursesByBatchSemesters(batchIds)
            
            val facultyCourses = batchCourses.filter { course ->
                course.instructors.contains(user)
            }

            facultyCourses.forEach { course ->
                if (!review.courses.contains(course)) {
                    review.courses.add(course)
                }
            }
        } else {
            batches.forEach { batch ->
                if (!review.batches.contains(batch)) {
                    review.batches.add(batch)
                }
            }
        }
    }

    fun removeBatchesFromReview(review: Review, batchIds: List<UUID>, user: User) {
        val batches = batchRepository.findAllById(batchIds)

        if (user.role == Role.FACULTY) {
            val batchCourses = courseRepository.findCoursesByBatchSemesters(batchIds)
            
            val facultyCourses = batchCourses.filter { course ->
                course.instructors.contains(user)
            }

            facultyCourses.forEach { course ->
                review.courses.remove(course)
            }
        } else {
            batches.forEach { batch ->
                review.batches.remove(batch)
            }
        }
    }

    fun addSemestersToReview(review: Review, semesterIds: List<UUID>, user: User) {
        val semesterCourses = courseRepository.findCoursesBySemesterIds(semesterIds)

        val coursesToAdd = if (user.role == Role.FACULTY) {
            semesterCourses.filter { course -> course.instructors.contains(user) }
        } else {
            semesterCourses
        }

        coursesToAdd.forEach { course ->
            if (!review.courses.contains(course)) {
                review.courses.add(course)
            }
        }
    }

    fun removeSemestersFromReview(review: Review, semesterIds: List<UUID>, user: User) {
        val semesterCourses = courseRepository.findCoursesBySemesterIds(semesterIds)

        val coursesToRemove = if (user.role == Role.FACULTY) {
            semesterCourses.filter { course -> course.instructors.contains(user) }
        } else {
            semesterCourses
        }
        
        coursesToRemove.forEach { course ->
            review.courses.remove(course)
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.REVIEW_DETAIL_CACHE], key = "'review_publication_' + #reviewId")
    fun getPublicationStatus(reviewId: UUID): ReviewPublicationResponse {
        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }

        val isFullyPublished = reviewPublicationHelper.isReviewFullyPublished(review.id!!)
        val publishedCourses = reviewPublicationHelper.getPublishedCoursesForReview(review)
        val latestPublication = if (publishedCourses.isNotEmpty()) {
            reviewCoursePublicationRepository.findByReview(review)
                .maxByOrNull { it.publishedAt }?.publishedAt
        } else null

        return ReviewPublicationResponse(
            reviewId = review.id!!,
            reviewName = review.name,
            isPublished = isFullyPublished,
            publishDate = latestPublication?.toLocalDate()
        )
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["review-detail"], allEntries = true),
        CacheEvict(value = ["reviews"], allEntries = true),
        CacheEvict(value = [CacheConfig.REVIEW_DETAIL_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.REVIEWS_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.PROJECT_REVIEWS_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.INDIVIDUAL_SCORES_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true),
        CacheEvict(value = [CacheConfig.COURSE_PERFORMANCE_CACHE, CacheConfig.COURSES_USER_CACHE], allEntries = true)
    ])
    fun publishReview(reviewId: UUID, userId: String): ReviewPublicationResponse {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }

        when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                publishAllCoursesForReview(review, user)
            }
            Role.FACULTY -> {
                publishFacultyCoursesForReview(review, user)
            }
            else -> {
                throw ForbiddenException("You don't have permission to publish reviews")
            }
        }

        val isFullyPublished = reviewPublicationHelper.isReviewFullyPublished(review.id!!)
        val latestPublication = reviewCoursePublicationRepository.findByReview(review)
            .maxByOrNull { it.publishedAt }?.publishedAt

        return ReviewPublicationResponse(
            reviewId = review.id!!,
            reviewName = review.name,
            isPublished = isFullyPublished,
            publishDate = latestPublication?.toLocalDate()
        )
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["review-detail"], allEntries = true),
        CacheEvict(value = ["reviews"], allEntries = true),
        CacheEvict(value = [CacheConfig.REVIEW_DETAIL_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.REVIEWS_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.PROJECT_REVIEWS_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.INDIVIDUAL_SCORES_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true),
        CacheEvict(value = [CacheConfig.COURSE_PERFORMANCE_CACHE, CacheConfig.COURSES_USER_CACHE], allEntries = true)
    ])
    fun unpublishReview(reviewId: UUID, userId: String): ReviewPublicationResponse {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        val review = reviewRepository.findById(reviewId).orElseThrow {
            NotFoundException("Review with id $reviewId not found")
        }

        when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                unpublishAllCoursesForReview(review)
            }
            Role.FACULTY -> {
                unpublishFacultyCoursesForReview(review, user)
            }
            else -> {
                throw ForbiddenException("You don't have permission to unpublish reviews")
            }
        }

        val isFullyPublished = reviewPublicationHelper.isReviewFullyPublished(review.id!!)
        val latestPublication = reviewCoursePublicationRepository.findByReview(review)
            .maxByOrNull { it.publishedAt }?.publishedAt

        return ReviewPublicationResponse(
            reviewId = review.id!!,
            reviewName = review.name,
            isPublished = isFullyPublished,
            publishDate = latestPublication?.toLocalDate()
        )
    }

    private fun publishAllCoursesForReview(review: Review, user: User) {
        val allCourseIds = reviewRepository.findAllCourseIdsForReview(review.id!!)
        val allCourses = courseRepository.findAllById(allCourseIds)
        
        logger.info("Publishing ${allCourses.size} courses for review ${review.id}")
        allCourses.forEach { course ->
            val existingPublication = reviewCoursePublicationRepository.findByReviewAndCourse(review, course)
            if (existingPublication == null) {
                val publication = ReviewCoursePublication(
                    review = review,
                    course = course,
                    publishedBy = user,
                    publishedAt = LocalDateTime.now()
                )
                reviewCoursePublicationRepository.save(publication)
            }
        }
    }

    private fun publishFacultyCoursesForReview(review: Review, faculty: User) {
        val facultyId = faculty.id ?: throw IllegalStateException("Faculty ID is null")
        
        val facultyCourseIds = reviewRepository.findFacultyCourseIdsForReview(review.id!!, facultyId)
        
        if (facultyCourseIds.isEmpty()) {
            logger.error("Faculty $facultyId (${faculty.name}) attempted to publish review ${review.id} (${review.name})")
            logger.error("No courses found where faculty is instructor for this review")
            throw ForbiddenException("Faculty can only publish courses they instruct")
        }

        val facultyCourses = courseRepository.findAllById(facultyCourseIds)
        
        logger.info("Faculty $facultyId (${faculty.name}) publishing ${facultyCourses.size} courses for review ${review.id}")
        facultyCourses.forEach { course ->
            logger.debug("Publishing course ${course.id} (${course.name})")
            val existingPublication = reviewCoursePublicationRepository.findByReviewAndCourse(review, course)
            if (existingPublication == null) {
                val publication = ReviewCoursePublication(
                    review = review,
                    course = course,
                    publishedBy = faculty,
                    publishedAt = LocalDateTime.now()
                )
                reviewCoursePublicationRepository.save(publication)
            }
        }
    }

    private fun unpublishAllCoursesForReview(review: Review) {
        reviewCoursePublicationRepository.deleteByReview(review)
    }

    private fun unpublishFacultyCoursesForReview(review: Review, faculty: User) {
        val facultyId = faculty.id ?: throw IllegalStateException("Faculty ID is null")
        
        val facultyCourseIds = reviewRepository.findFacultyCourseIdsForReview(review.id!!, facultyId)
        
        if (facultyCourseIds.isEmpty()) {
            logger.error("Faculty $facultyId (${faculty.name}) attempted to unpublish review ${review.id} (${review.name})")
            logger.error("No courses found where faculty is instructor for this review")
            throw ForbiddenException("Faculty can only unpublish courses they instruct")
        }

        val facultyCourses = courseRepository.findAllById(facultyCourseIds)
        
        logger.info("Faculty $facultyId (${faculty.name}) unpublishing ${facultyCourses.size} courses for review ${review.id}")
        facultyCourses.forEach { course ->
            logger.debug("Unpublishing course ${course.id} (${course.name})")
            reviewCoursePublicationRepository.deleteByReviewAndCourse(review, course)
        }
    }
}
