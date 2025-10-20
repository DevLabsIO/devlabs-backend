package com.devlabs.devlabsbackend.review.service

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.repository.ReviewCoursePublicationRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ReviewPublicationHelper(
private val reviewCoursePublicationRepository: ReviewCoursePublicationRepository,
private val courseRepository: CourseRepository
) {

@Transactional(readOnly = true)
fun isReviewPublishedForUser(review: Review, user: User): Boolean {
    return isReviewPublishedForUser(review.id!!, user.id!!, user.role.name)
}

@Transactional(readOnly = true)
fun isReviewPublishedForUser(reviewId: UUID, userId: String, role: String): Boolean {
    return reviewCoursePublicationRepository.isPublishedForUser(reviewId, userId, role)
}

@Transactional(readOnly = true)
fun areReviewsPublishedForUser(reviewIds: List<UUID>, userId: String, role: String): Map<UUID, Boolean> {
    if (reviewIds.isEmpty()) return emptyMap()
    val results = reviewCoursePublicationRepository.arePublishedForUser(reviewIds, userId, role)
    return results.associate { 
        UUID.fromString(it["review_id"].toString()) to (it["is_published"] as Boolean)
    }
}

@Transactional(readOnly = true)
fun isReviewPublishedForCourse(review: Review, course: Course): Boolean {
    return reviewCoursePublicationRepository.isPublishedForCourse(review.id!!, course.id!!)
}

@Transactional(readOnly = true)
fun isReviewPublishedForCourse(reviewId: UUID, courseId: UUID): Boolean {
    return reviewCoursePublicationRepository.isPublishedForCourse(reviewId, courseId)
}

@Transactional(readOnly = true)
fun isReviewFullyPublished(review: Review): Boolean {
    val reviewId = review.id!!
    val totalCourses = reviewCoursePublicationRepository.getTotalCourseCount(reviewId)
    if (totalCourses == 0) return false
    val publishedCourses = reviewCoursePublicationRepository.getPublishedCourseCount(reviewId)
    return publishedCourses == totalCourses
}

@Transactional(readOnly = true)
fun isReviewFullyPublished(reviewId: UUID): Boolean {
    val totalCourses = reviewCoursePublicationRepository.getTotalCourseCount(reviewId)
    if (totalCourses == 0) return false
    val publishedCourses = reviewCoursePublicationRepository.getPublishedCourseCount(reviewId)
    return publishedCourses == totalCourses
}

@Transactional(readOnly = true)
fun isReviewPartiallyPublished(review: Review): Boolean {
    return reviewCoursePublicationRepository.getPublishedCourseCount(review.id!!) > 0
}

@Transactional(readOnly = true)
fun isReviewPartiallyPublished(reviewId: UUID): Boolean {
    return reviewCoursePublicationRepository.getPublishedCourseCount(reviewId) > 0
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

@Transactional(readOnly = true)
fun getPublishableCourseIds(reviewId: UUID, userId: String, role: String): List<UUID> {
    return reviewCoursePublicationRepository.getPublishableCourseIds(reviewId, userId, role)
}

fun getPublishableCoursesForUser(user: User, review: Review): List<Course> {
    val courseIds = getPublishableCourseIds(review.id!!, user.id!!, user.role.name)
    return if (courseIds.isEmpty()) {
        emptyList()
    } else {
        courseRepository.findAllById(courseIds)
    }
}

}
