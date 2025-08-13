package com.devlabs.devlabsbackend.dashboard.service

import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.review.repository.ReviewRepository
import com.devlabs.devlabsbackend.review.repository.ReviewCoursePublicationRepository
import com.devlabs.devlabsbackend.semester.repository.SemesterRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import com.devlabs.devlabsbackend.user.repository.UserRepository
import com.devlabs.devlabsbackend.project.repository.ProjectRepository
import com.devlabs.devlabsbackend.project.domain.ProjectStatus
import com.devlabs.devlabsbackend.individualscore.repository.IndividualScoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class DashboardService(
    private val userRepository: UserRepository,
    private val courseRepository: CourseRepository,
    private val batchRepository: BatchRepository,
    private val semesterRepository: SemesterRepository,
    private val reviewRepository: ReviewRepository,
    private val reviewCoursePublicationRepository: ReviewCoursePublicationRepository,
    private val projectRepository: ProjectRepository,
    private val individualScoreRepository: IndividualScoreRepository
) {

    @Transactional(readOnly = true)
    fun getAdminDashboard(): AdminDashboardResponse {
        return try {
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
            
            AdminDashboardResponse(
                userStats = UserStatsResponse(totalUsers, totalStudents, totalFaculty, totalManagers),
                semesterStats = SemesterStatsResponse(totalSemesters, activeSemesters),
                courseStats = CourseStatsResponse(totalCourses, activeCourses),
                batchStats = BatchStatsResponse(totalBatches, activeBatches),
                recentUsers = recentUsers.map { it.toUserSummary() }
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch admin dashboard data", e)
        }
    }

    @Transactional(readOnly = true)
    fun getManagerStaffDashboard(userId: String): ManagerStaffDashboardResponse {
        return try {
            val user = userRepository.findById(userId).orElseThrow { 
                RuntimeException("User with id $userId not found") 
            }
            
            val today = LocalDate.now()
            
            // Manager/Staff sees ALL data across the entire system - they have oversight of everything
            
            // Get ALL reviews from the system
            val allReviews = reviewRepository.findAll()
            
            val totalReviews = allReviews.size
            val activeReviews = allReviews.count { review ->
                !today.isBefore(review.startDate) && !today.isAfter(review.endDate)
            }
            val completedReviews = allReviews.count { review ->
                today.isAfter(review.endDate)
            }
            
            // Get upcoming reviews (next 5) from ALL reviews
            val upcomingReviews = allReviews
                .filter { review -> today.isBefore(review.startDate) }
                .sortedBy { it.startDate }
                .take(5)
                .map { it.toReviewSummary() }
            
            val allCourses = courseRepository.findAll()
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
            
            val allProjects = projectRepository.findAll()
            val totalProjects = allProjects.size
            val activeProjects = allProjects.count { project ->
                project.status in listOf(ProjectStatus.ONGOING, ProjectStatus.PROPOSED)
            }
            
            ManagerStaffDashboardResponse(
                totalReviews = totalReviews,
                activeReviews = activeReviews,
                completedReviews = completedReviews,
                totalProjects = totalProjects,
                activeProjects = activeProjects,
                upcomingReviews = upcomingReviews,
                recentlyPublishedReviews = recentlyPublishedReviews
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch manager/staff dashboard data", e)
        }
    }

    @Transactional(readOnly = true)
    fun getStudentDashboard(userId: String): StudentDashboardResponse {
        return try {
            val student = userRepository.findById(userId).orElseThrow { 
                RuntimeException("Student with id $userId not found") 
            }
            
            if (student.role != Role.STUDENT) {
                throw RuntimeException("User with id $userId is not a student")
            }
            
            val today = LocalDate.now()
            
            val enrolledCourses = courseRepository.findCoursesByActiveSemestersAndStudent(student)
            val batchCourses = courseRepository.findCoursesByActiveSemestersAndStudentThroughBatch(student)
            val allCourses = (enrolledCourses + batchCourses).distinctBy { it.id }
            
            val allReviews = if (allCourses.isNotEmpty()) {
                reviewRepository.findByCoursesIn(allCourses)
            } else {
                emptyList()
            }
            
            val totalReviews = allReviews.size
            val activeReviews = allReviews.count { review ->
                !today.isBefore(review.startDate) && !today.isAfter(review.endDate)
            }
            val completedReviews = allReviews.count { review ->
                today.isAfter(review.endDate)
            }
            

            val upcomingReviews = allReviews
                .filter { review -> today.isBefore(review.startDate) }
                .sortedBy { it.startDate }
                .take(5)
                .map { it.toReviewSummary() }
            
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
            
            StudentDashboardResponse(
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
        } catch (e: Exception) {
            throw RuntimeException("Failed to fetch student dashboard data", e)
        }
    }
    
    
    private fun calculateAverageProjectScore(student: User, courses: List<com.devlabs.devlabsbackend.course.domain.Course>): Double {
        return try {
            if (courses.isEmpty()) return 0.0
            
            val scores = courses.mapNotNull { course ->
                val scores = individualScoreRepository.findByParticipantAndProjectInCourse(student, course)
                if (scores.isNotEmpty()) {
                    scores.map { it.score }.average()
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
    val role: Role,
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
    val courseName: String,
    val publishedAt: String,
    val publishedBy: String
)

// Extension functions
fun User.toUserSummary() = UserSummaryResponse(
    id = id!!,
    name = name,
    email = email,
    role = role,
    createdAt = createdAt.toString()
)

fun com.devlabs.devlabsbackend.review.domain.Review.toReviewSummary() = ReviewSummaryResponse(
    id = id!!,
    name = name,
    startDate = startDate,
    endDate = endDate,
    courseName = courses.firstOrNull()?.name ?: "Multiple Courses"
)

fun com.devlabs.devlabsbackend.review.domain.ReviewCoursePublication.toPublishedReviewSummary() = PublishedReviewSummaryResponse(
    reviewId = review.id!!,
    reviewName = review.name,
    courseName = course.name,
    publishedAt = publishedAt.toString(),
    publishedBy = publishedBy.name
)
