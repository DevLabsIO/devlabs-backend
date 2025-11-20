package com.devlabs.devlabsbackend.dashboard.service

import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.dashboard.domain.dto.*
import com.devlabs.devlabsbackend.individualscore.repository.IndividualScoreRepository
import com.devlabs.devlabsbackend.project.domain.ProjectStatus
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.review.repository.ReviewCoursePublicationRepository
import com.devlabs.devlabsbackend.review.repository.ReviewRepository
import com.devlabs.devlabsbackend.review.service.ReviewPublicationHelper
import com.devlabs.devlabsbackend.semester.repository.SemesterRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val batchRepository: BatchRepository,
    private val semesterRepository: SemesterRepository,
    private val reviewRepository: ReviewRepository,
    private val reviewCoursePublicationRepository: ReviewCoursePublicationRepository,
    private val projectRepository: ProjectRepository,
    private val individualScoreRepository: IndividualScoreRepository,
    private val reviewPublicationHelper: ReviewPublicationHelper
) {
    private val logger = LoggerFactory.getLogger(DashboardService::class.java)

    @Cacheable(cacheNames = [CacheConfig.DASHBOARD_ADMIN], key = "'admin'")
    fun getAdminDashboard(): AdminDashboardResponse {
        val totalUsers = userRepository.count()
        val totalStudents = userRepository.countByRole(Role.STUDENT)
        val totalFaculty = userRepository.countByRole(Role.FACULTY)
        val totalManagers = userRepository.countByRole(Role.MANAGER)
        
        val totalSemesters = semesterRepository.count()
        val activeSemesters = semesterRepository.countByIsActive(true)
        
        val totalCourses = courseRepository.count()
        val activeCourses = courseRepository.countByActiveSemesters()
        
        val totalBatches = batchRepository.count()
        val activeBatches = batchRepository.countByIsActive(true)
        
        val recentUsers = userRepository.findTop5ByOrderByCreatedAtDesc()
        
        return AdminDashboardResponse(
            userStats = UserStatsResponse(totalUsers, totalStudents, totalFaculty, totalManagers),
            semesterStats = SemesterStatsResponse(totalSemesters, activeSemesters),
            courseStats = CourseStatsResponse(totalCourses, activeCourses),
            batchStats = BatchStatsResponse(totalBatches, activeBatches),
            recentUsers = recentUsers.map { it.toUserSummary() }
        )
    }

    @Cacheable(cacheNames = [CacheConfig.DASHBOARD_MANAGER], key = "#userId")
    fun getManagerStaffDashboard(userId: String): ManagerStaffDashboardResponse {
        userRepository.findById(userId).orElseThrow { 
            NotFoundException("User with id $userId not found") 
        }
        
        val today = LocalDate.now()
        
        val reviewStats = reviewRepository.getReviewStats(today)
        val totalReviews = (reviewStats["total_count"] as? Number)?.toInt() ?: 0
        val activeReviews = (reviewStats["active_count"] as? Number)?.toInt() ?: 0
        val completedReviews = (reviewStats["completed_count"] as? Number)?.toInt() ?: 0
        
        val upcomingReviewsData = reviewRepository.findUpcomingReviews(today)
        val upcomingReviews = upcomingReviewsData.map { row ->
            ReviewSummaryResponse(
                id = UUID.fromString(row["id"].toString()),
                name = row["name"].toString(),
                startDate = (row["start_date"] as java.sql.Date).toLocalDate(),
                endDate = (row["end_date"] as java.sql.Date).toLocalDate(),
                courseName = "Multiple Courses" // Simplified for dashboard
            )
        }
        
        val recentPublicationsData = reviewCoursePublicationRepository.findRecentPublicationsNative()
        val recentlyPublishedReviews = recentPublicationsData.map { row ->
            PublishedReviewSummaryResponse(
                reviewId = UUID.fromString(row["review_id"].toString()),
                reviewName = row["review_name"].toString(),
                publishedAt = row["published_at"].toString()
            )
        }
        
        val totalProjects = projectRepository.count().toInt()
        val activeProjects = projectRepository.countByStatusIn(
            listOf(ProjectStatus.ONGOING, ProjectStatus.PROPOSED)
        ).toInt()
        
        return ManagerStaffDashboardResponse(
            totalReviews = totalReviews,
            activeReviews = activeReviews,
            completedReviews = completedReviews,
            totalProjects = totalProjects,
            activeProjects = activeProjects,
            upcomingReviews = upcomingReviews,
            recentlyPublishedReviews = recentlyPublishedReviews
        )
    }
    
    @Cacheable(cacheNames = [CacheConfig.DASHBOARD_STUDENT], key = "#userId")
    fun getStudentDashboard(userId: String): StudentDashboardResponse {
        val student = userRepository.findById(userId).orElseThrow { 
            NotFoundException("Student with id $userId not found") 
        }
        
        if (student.role != Role.STUDENT) {
            throw IllegalArgumentException("User with id $userId is not a student")
        }
        
        val today = LocalDate.now()
        
        val enrolledCourses = courseRepository.findCoursesByActiveSemestersAndStudent(student)
        val batchCourses = courseRepository.findCoursesByActiveSemestersAndStudentThroughBatch(student)
        val allCourses = (enrolledCourses + batchCourses).distinctBy { it.id }
        
        val allReviewData = if (allCourses.isNotEmpty()) {
            val publishedReviews = reviewCoursePublicationRepository.findReviewsByCourses(allCourses)
            val publishedReviewIds = publishedReviews.map { it.review.id!! }.distinct()
            
            if (publishedReviewIds.isNotEmpty()) {
                reviewRepository.findReviewDataByIds(publishedReviewIds)
            } else {
                emptyList()
            }
        } else {
            emptyList()
        }
        
        val totalReviews = allReviewData.size
        val activeReviews = allReviewData.count { reviewMap ->
            val startDate = (reviewMap["start_date"] as java.sql.Date).toLocalDate()
            val endDate = (reviewMap["end_date"] as java.sql.Date).toLocalDate()
            !today.isBefore(startDate) && !today.isAfter(endDate)
        }
        val completedReviews = allReviewData.count { reviewMap ->
            val endDate = (reviewMap["end_date"] as java.sql.Date).toLocalDate()
            today.isAfter(endDate)
        }
        
        val upcomingReviews = allReviewData
            .filter { reviewMap -> 
                val startDate = (reviewMap["start_date"] as java.sql.Date).toLocalDate()
                today.isBefore(startDate)
            }
            .sortedBy { (it["start_date"] as java.sql.Date).toLocalDate() }
            .take(5)
            .map { reviewMap ->
                val reviewId = reviewMap["id"] as UUID
                val courseName = allCourses.firstOrNull()?.name ?: "N/A"
                ReviewSummaryResponse(
                    id = reviewId,
                    name = reviewMap["name"] as String,
                    startDate = (reviewMap["start_date"] as java.sql.Date).toLocalDate(),
                    endDate = (reviewMap["end_date"] as java.sql.Date).toLocalDate(),
                    courseName = courseName
                )
            }
        
        val recentlyPublishedReviews = if (allCourses.isNotEmpty()) {
            reviewCoursePublicationRepository.findRecentPublicationsByCourses(allCourses)
                .groupBy { it.review.id }
                .map { (_, publications) -> 
                    publications.maxByOrNull { it.publishedAt }!!
                }
                .sortedByDescending { it.publishedAt }
                .take(5)
                .map { it.toPublishedReviewSummary() }
        } else {
            emptyList()
        }
        
        val activeProjects = projectRepository.findByTeamMembersContainingAndStatusIn(
            student, listOf(ProjectStatus.ONGOING, ProjectStatus.PROPOSED)
        )
        val completedProjects = projectRepository.findByTeamMembersContainingAndStatusIn(
            student, listOf(ProjectStatus.COMPLETED)
        )
        val totalProjects = activeProjects.size + completedProjects.size
        
        val averageProjectScore = calculateAverageProjectScore(student, allCourses)
        
        return StudentDashboardResponse(
            totalReviews = totalReviews,
            activeReviews = activeReviews,
            completedReviews = completedReviews,
            totalProjects = totalProjects,
            activeProjects = activeProjects.size,
            completedProjects = completedProjects.size,
            averageProjectScore = averageProjectScore,
            upcomingReviews = upcomingReviews,
            recentlyPublishedReviews = recentlyPublishedReviews
        )
    }
    
    private fun calculateAverageProjectScore(student: User, courses: List<Course>): Double {
        return try {
            if (courses.isEmpty()) return 0.0
            
            val allScoresByCourse = courses.associateWith { course ->
                individualScoreRepository.findByParticipantAndProjectInCourse(student, course)
            }
            
            val allReviewIds = allScoresByCourse.values.flatten()
                .mapNotNull { it.review.id }
                .distinct()
            
            val publicationMap = if (allReviewIds.isNotEmpty()) {
                reviewPublicationHelper.areReviewsPublishedForUser(allReviewIds, student.id!!, student.role.name)
            } else {
                emptyMap()
            }
            
            val scores = courses.mapNotNull { course ->
                val allScores = allScoresByCourse[course] ?: emptyList()
                val publishedScores = allScores.filter { score ->
                    publicationMap[score.review.id] == true
                }
                
                if (publishedScores.isNotEmpty()) {
                    publishedScores.map { it.score }.average()
                } else {
                    null
                }
            }
            
            if (scores.isNotEmpty()) scores.average() else 0.0
        } catch (e: Exception) {
            0.0
        }
    }
}
