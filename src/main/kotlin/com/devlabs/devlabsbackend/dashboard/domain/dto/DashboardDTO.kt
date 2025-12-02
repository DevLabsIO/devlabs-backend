package com.devlabs.devlabsbackend.dashboard.domain.dto

import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.review.domain.ReviewCoursePublication
import com.devlabs.devlabsbackend.user.domain.User
import java.time.LocalDate
import java.util.*

data class AdminDashboardResponse(
    val userStats: UserStatsResponse,
    val semesterStats: SemesterStatsResponse,
    val courseStats: CourseStatsResponse,
    val batchStats: BatchStatsResponse,
    val recentUsers: List<UserSummaryResponse>
)

data class UserStatsResponse(
    val total: Long,
    val students: Long,
    val faculty: Long,
    val managers: Long
)

data class SemesterStatsResponse(
    val total: Long,
    val active: Long
)

data class CourseStatsResponse(
    val total: Long,
    val active: Long
)

data class BatchStatsResponse(
    val total: Long,
    val active: Long
)

data class UserSummaryResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,  // ✅ Changed from Role enum to String for Redis serialization
    val createdAt: String
)

data class ManagerStaffDashboardResponse(
    val totalReviews: Int,
    val activeReviews: Int,
    val completedReviews: Int,
    val totalProjects: Int,
    val activeProjects: Int,
    val upcomingReviews: List<ReviewSummaryResponse>,
    val recentlyPublishedReviews: List<PublishedReviewSummaryResponse>
)

data class StudentDashboardResponse(
    val totalReviews: Int,
    val activeReviews: Int,
    val completedReviews: Int,
    val totalProjects: Int,
    val activeProjects: Int,
    val completedProjects: Int,
    val averageProjectScore: Double,
    val upcomingReviews: List<ReviewSummaryResponse>,
    val recentlyPublishedReviews: List<PublishedReviewSummaryResponse>
)

data class ReviewSummaryResponse(
    val id: UUID,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val courseName: String
)

data class PublishedReviewSummaryResponse(
    val reviewId: UUID,
    val reviewName: String,
    val publishedAt: String
)

fun User.toUserSummary() = UserSummaryResponse(
    id = id ?: throw IllegalStateException("User ID cannot be null"),
    name = name,
    email = email,
    role = role.name,  // ✅ Convert enum to String
    createdAt = createdAt.toString()
)

fun ReviewCoursePublication.toPublishedReviewSummary() = PublishedReviewSummaryResponse(
    reviewId = review.id ?: throw IllegalStateException("Review ID cannot be null"),
    reviewName = review.name,
    publishedAt = publishedAt.toString()
)
