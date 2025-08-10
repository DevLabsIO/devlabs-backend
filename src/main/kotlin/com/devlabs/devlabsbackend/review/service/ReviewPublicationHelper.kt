package com.devlabs.devlabsbackend.review.service

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.repository.ReviewCoursePublicationRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewPublicationHelper(
    private val reviewCoursePublicationRepository: ReviewCoursePublicationRepository
) {

    @Transactional(readOnly = true)
    fun isReviewPublishedForUser(review: Review, user: User): Boolean {
        return when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
                isReviewFullyPublished(review)
            }
            Role.FACULTY -> {
                val userCourses = review.courses.filter { course ->
                    course.instructors.contains(user)
                }
                userCourses.any { course ->
                    isReviewPublishedForCourse(review, course)
                }
            }
            Role.STUDENT -> {
                val userCourses = review.courses.filter { course ->
                    course.students.contains(user)
                }
                userCourses.any { course ->
                    isReviewPublishedForCourse(review, course)
                }
            }
        }
    }

    @Transactional(readOnly = true)
    fun isReviewPublishedForCourse(review: Review, course: Course): Boolean {
        return reviewCoursePublicationRepository.findByReviewAndCourse(review, course) != null
    }

    @Transactional(readOnly = true)
    fun isReviewFullyPublished(review: Review): Boolean {
        if (review.courses.isEmpty()) return false
        
        val publishedCourses = reviewCoursePublicationRepository.findPublishedCoursesByReview(review)
        return publishedCourses.size == review.courses.size
    }

    @Transactional(readOnly = true)
    fun isReviewPartiallyPublished(review: Review): Boolean {
        val publishedCourses = reviewCoursePublicationRepository.findPublishedCoursesByReview(review)
        return publishedCourses.isNotEmpty()
    }

    @Transactional(readOnly = true)
    fun getPublishedCoursesForReview(review: Review): List<Course> {
        return reviewCoursePublicationRepository.findPublishedCoursesByReview(review)
    }

    fun canUserPublishCourse(user: User, review: Review, course: Course): Boolean {
        return when (user.role) {
            Role.ADMIN, Role.MANAGER -> true
            Role.FACULTY -> course.instructors.contains(user)
            else -> false
        }
    }

    fun getPublishableCoursesForUser(user: User, review: Review): List<Course> {
        return when (user.role) {
            Role.ADMIN, Role.MANAGER -> review.courses.toList()
            Role.FACULTY -> review.courses.filter { course ->
                course.instructors.contains(user)
            }
            else -> emptyList()
        }
    }
}
