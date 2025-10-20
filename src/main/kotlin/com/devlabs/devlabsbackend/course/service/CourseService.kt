package com.devlabs.devlabsbackend.course.service

import com.devlabs.devlabsbackend.batch.domain.dto.BatchResponse
import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.course.domain.dto.CoursePerformanceChartResponse
import com.devlabs.devlabsbackend.course.domain.dto.CourseResponse
import com.devlabs.devlabsbackend.course.domain.dto.StudentCourseWithScoresResponse
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.individualscore.repository.IndividualScoreRepository
import com.devlabs.devlabsbackend.review.service.ReviewPublicationHelper
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import com.devlabs.devlabsbackend.user.domain.dto.UserResponse
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Year
import java.util.*

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val batchRepository: BatchRepository,
    private val individualScoreRepository: IndividualScoreRepository,
    private val reviewPublicationHelper: ReviewPublicationHelper
) {
    
    private val logger = LoggerFactory.getLogger(CourseService::class.java)

    @Cacheable(value = [CacheConfig.COURSE_DETAIL_CACHE], key = "#courseId")
    @Transactional(readOnly = true)
    fun getCourseById(courseId: UUID): CourseResponse {
        val courseData = courseRepository.findCourseListData(listOf(courseId))
        if (courseData.isEmpty()) {
            throw NotFoundException("Course with id $courseId not found")
        }
        return mapToCourseResponse(courseData.first())
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.COURSE_BATCHES_CACHE, CacheConfig.COURSE_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun addBatchesToCourse(courseId: UUID, batchId: List<UUID>) {
        val course = courseRepository.findByIdWithBatches(courseId)
            ?: throw NotFoundException("Could not find course with id $courseId")
        val batches = batchRepository.findAllById(batchId)
        course.batches.addAll(batches)
        courseRepository.save(course)
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.COURSE_BATCHES_CACHE, CacheConfig.COURSE_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun removeBatchesFromCourse(courseId: UUID, batchId: List<UUID>) {
        val course = courseRepository.findByIdWithBatches(courseId)
            ?: throw NotFoundException("Could not find course with id $courseId")
        val batches = batchRepository.findAllById(batchId)
        course.batches.removeAll(batches)
        courseRepository.save(course)
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.COURSE_STUDENTS_CACHE, CacheConfig.COURSE_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSES_USER_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun assignStudents(courseId: UUID, studentId: List<String>) {
        val course = courseRepository.findByIdWithStudents(courseId)
            ?: throw NotFoundException("Could not find course with id $courseId")
        val users = userRepository.findAllById(studentId)
        course.students.addAll(users)
        courseRepository.save(course)
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.COURSE_STUDENTS_CACHE, CacheConfig.COURSE_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSES_USER_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun removeStudents(courseId: UUID, studentId: List<String>) {
        val course = courseRepository.findByIdWithStudents(courseId)
            ?: throw NotFoundException("Could not find course with id $courseId")
        val users = userRepository.findAllById(studentId)
        course.students.removeAll(users)
        courseRepository.save(course)
    }

    @Cacheable(value = [CacheConfig.COURSE_INSTRUCTORS_CACHE], key = "#courseId")
    @Transactional(readOnly = true)
    fun getCourseInstructors(courseId: UUID): List<UserResponse> {
        val instructors = courseRepository.findInstructorsByCourseId(courseId)
        return instructors.map { mapToUserResponse(it) }
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.COURSE_INSTRUCTORS_CACHE, CacheConfig.COURSE_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSES_ACTIVE_CACHE, CacheConfig.COURSES_USER_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_MANAGER], allEntries = true)
        ]
    )
    fun assignInstructors(courseId: UUID, instructorId: List<String>) {
        val course = courseRepository.findByIdWithInstructors(courseId)
            ?: throw NotFoundException("Could not find course with id $courseId")
        val users = userRepository.findAllById(instructorId)
        course.instructors.addAll(users)
        courseRepository.save(course)
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.COURSE_INSTRUCTORS_CACHE, CacheConfig.COURSE_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSES_ACTIVE_CACHE, CacheConfig.COURSES_USER_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_MANAGER], allEntries = true)
        ]
    )
    fun removeInstructors(courseId: UUID, instructorId: List<String>) {
        val course = courseRepository.findByIdWithInstructors(courseId)
            ?: throw NotFoundException("Could not find course with id $courseId")
        val users = userRepository.findAllById(instructorId)
        course.instructors.removeAll(users)
        courseRepository.save(course)
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.COURSES_ACTIVE_CACHE], key = "'all'")
    fun getAllActiveCourses(): List<CourseResponse> {
        val coursesData = courseRepository.findAllActiveCourses()
        return coursesData.map { mapToCourseResponse(it) }
    }

    @Cacheable(value = [CacheConfig.COURSES_USER_CACHE], key = "'user-' + #userId")
    @Transactional(readOnly = true)
    fun getActiveCoursesForUser(userId: String): List<CourseResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }
        
        val coursesData = when (user.role) {
            Role.STUDENT -> {
                val directCourses = courseRepository.findActiveCoursesByStudent(userId)
                if (directCourses.isEmpty()) {
                    courseRepository.findActiveCoursesByStudentThroughBatch(userId)
                } else {
                    directCourses
                }
            }
            Role.FACULTY -> {
                courseRepository.findActiveCoursesByInstructor(userId)
            }
            Role.ADMIN, Role.MANAGER -> {
                courseRepository.findAllActiveCourses()
            }
        }
        
        return coursesData.map { mapToCourseResponse(it) }
    }

    @Cacheable(value = [CacheConfig.COURSES_USER_CACHE], key = "'student-scores-' + #studentId")
    @Transactional(readOnly = true)
    fun getStudentActiveCoursesWithScores(studentId: String): List<StudentCourseWithScoresResponse> {
        val student = userRepository.findById(studentId).orElseThrow {
            NotFoundException("Student with id $studentId not found")
        }

        if (student.role != Role.STUDENT) {
            throw IllegalArgumentException("User is not a student")
        }

        val courses = courseRepository.findCoursesByActiveSemestersAndStudent(student)
        val batchCourses = courseRepository.findCoursesByActiveSemestersAndStudentThroughBatch(student)
        val finalCourses = if (courses.isEmpty() && batchCourses.isNotEmpty()) {
            batchCourses
        } else {
            courses
        }

        return finalCourses.map { course ->
            val allReviews = individualScoreRepository.findDistinctReviewsByParticipantAndCourse(student, course)
            val publishedReviews = allReviews.filter { review ->
                reviewPublicationHelper.isReviewPublishedForUser(review, student)
            }

            val reviewCount = publishedReviews.size
            val averageScorePercentage = if (reviewCount == 0) {
                100.0
            } else {
                val allPublishedScores = publishedReviews.flatMap { review ->
                    individualScoreRepository.findByParticipantAndReviewAndCourse(student, review, course)
                }

                if (allPublishedScores.isEmpty()) {
                    100.0
                } else {
                    val totalPossibleScore = allPublishedScores.sumOf { it.criterion.maxScore.toDouble() }
                    val actualScore = allPublishedScores.sumOf { it.score }
                    if (totalPossibleScore > 0.0) {
                        (actualScore / totalPossibleScore) * 100.0
                    } else {
                        100.0
                    }
                }
            }

            StudentCourseWithScoresResponse(
                id = course.id!!,
                name = course.name,
                code = course.code,
                description = course.description,
                averageScorePercentage = averageScorePercentage,
                reviewCount = reviewCount
            )
        }
    }

    @Cacheable(value = [CacheConfig.COURSE_PERFORMANCE_CACHE], key = "'student-' + #studentId + '-course-' + #courseId")
    @Transactional(readOnly = true)
    fun getStudentCoursePerformanceChart(studentId: String, courseId: UUID): List<CoursePerformanceChartResponse> {
        val student = userRepository.findById(studentId).orElseThrow {
            NotFoundException("Student with id $studentId not found")
        }

        val course = courseRepository.findById(courseId).orElseThrow {
            NotFoundException("Course with id $courseId not found")
        }

        if (student.role != Role.STUDENT) {
            throw IllegalArgumentException("User is not a student")
        }

        val isDirectlyEnrolled = courseRepository.isStudentEnrolledInCourse(courseId, studentId)
        val isEnrolledThroughBatch = courseRepository.isStudentEnrolledThroughBatch(courseId, studentId)

        if (!isDirectlyEnrolled && !isEnrolledThroughBatch) {
            throw IllegalArgumentException("Student is not enrolled in this course")
        }

        val allReviews = individualScoreRepository.findDistinctReviewsByParticipantAndCourse(student, course)
        val publishedReviews = allReviews.filter { review ->
            reviewPublicationHelper.isReviewPublishedForUser(review, student)
        }

        if (publishedReviews.isEmpty()) {
            return emptyList()
        }

        return publishedReviews.map { review ->
            val scores = individualScoreRepository.findByParticipantAndReviewAndCourse(student, review, course)
            val totalPossibleScore = scores.sumOf { score -> score.criterion.maxScore.toDouble() }
            val actualScore = scores.sumOf { score -> score.score }
            val scorePercentage = if (totalPossibleScore > 0.0) {
                (actualScore / totalPossibleScore) * 100.0
            } else {
                0.0
            }

            val currentDate = java.time.LocalDate.now()
            val status = when {
                scores.isEmpty() && review.endDate.isBefore(currentDate) -> "missed"
                scores.isNotEmpty() -> "completed"
                review.startDate.isAfter(currentDate) -> "upcoming"
                else -> "ongoing"
            }

            CoursePerformanceChartResponse(
                reviewId = review.id!!,
                reviewName = review.name,
                startDate = review.startDate,
                endDate = review.endDate,
                status = status,
                showResult = true,
                score = if (scores.isNotEmpty()) actualScore else null,
                totalScore = if (scores.isNotEmpty()) totalPossibleScore else null,
                scorePercentage = if (scores.isNotEmpty()) scorePercentage else null,
                courseName = course.name,
                courseCode = course.code
            )
        }.sortedBy { it.startDate }
    }

    @Transactional(readOnly = true)
    fun getCoursesForCurrentUser(
        currentUser: User,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<CourseResponse> {
        val actualSortOrder = sortOrder.uppercase()
        val offset = page * size

        val (ids, totalCount) = when (currentUser.role) {
            Role.MANAGER -> {
                val ids = courseRepository.findActiveCourseIdsOnly(sortBy, actualSortOrder, offset, size)
                val count = courseRepository.countActiveCourses()
                ids to count
            }
            Role.FACULTY -> {
                val ids = courseRepository.findActiveCourseIdsByInstructor(currentUser.id!!, sortBy, actualSortOrder, offset, size)
                val count = courseRepository.countActiveCoursesByInstructor(currentUser.id!!)
                ids to count
            }
            else -> {
                throw IllegalArgumentException("Access denied. Only MANAGER and FACULTY roles can access this endpoint.")
            }
        }

        if (ids.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }

        val coursesData = courseRepository.findCourseListData(ids)
        val courseResponses = coursesData.map { mapToCourseResponse(it) }

        return PaginatedResponse(
            data = courseResponses,
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }

    @Transactional(readOnly = true)
    fun searchCoursesForCurrentUser(
        currentUser: User,
        query: String,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<CourseResponse> {
        val actualSortOrder = sortOrder.uppercase()
        val offset = page * size

        val (ids, totalCount) = when (currentUser.role) {
            Role.MANAGER -> {
                val ids = courseRepository.searchActiveCourseIdsOnly(query, sortBy, actualSortOrder, offset, size)
                val count = courseRepository.countSearchActiveCourses(query)
                ids to count
            }
            Role.FACULTY -> {
                val ids = courseRepository.searchActiveCourseIdsByInstructor(currentUser.id!!, query, sortBy, actualSortOrder, offset, size)
                val count = courseRepository.countSearchActiveCoursesByInstructor(currentUser.id!!, query)
                ids to count
            }
            else -> {
                throw IllegalArgumentException("Access denied. Only MANAGER and FACULTY roles can access this endpoint.")
            }
        }

        if (ids.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0
                )
            )
        }

        val coursesData = courseRepository.findCourseListData(ids)
        val courseResponses = coursesData.map { mapToCourseResponse(it) }

        return PaginatedResponse(
            data = courseResponses,
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }

    @Cacheable(
        value = [CacheConfig.COURSE_STUDENTS_CACHE], 
        key = "'course-' + #courseId + '-page-' + #page + '-size-' + #size + '-sort-' + #sortBy + '-' + #sortOrder",
        condition = "#page == 0 && #size == 10 && #sortBy == 'name' && #sortOrder == 'asc'"
    )
    @Transactional(readOnly = true)
    fun getCourseStudents(
        courseId: UUID,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<UserResponse> {
        if (!courseRepository.existsById(courseId)) {
            throw NotFoundException("Course with id $courseId not found")
        }

        val actualSortOrder = sortOrder.uppercase()
        val offset = page * size

        val students = courseRepository.findStudentsByCourseId(courseId, sortBy, actualSortOrder, offset, size)
        val totalCount = courseRepository.countStudentsByCourseId(courseId)
        
        return PaginatedResponse(
            data = students.map { mapToUserResponse(it) },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }

    @Cacheable(
        value = [CacheConfig.COURSE_BATCHES_CACHE], 
        key = "'course-' + #courseId + '-page-' + #page + '-size-' + #size + '-sort-' + #sortBy + '-' + #sortOrder",
        condition = "#page == 0 && #size == 10 && #sortBy == 'name' && #sortOrder == 'asc'"
    )
    @Transactional(readOnly = true)
    fun getCourseBatches(
        courseId: UUID,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<BatchResponse> {
        if (!courseRepository.existsById(courseId)) {
            throw NotFoundException("Course with id $courseId not found")
        }

        val actualSortOrder = sortOrder.uppercase()
        val offset = page * size
        
        val batches = courseRepository.findBatchesByCourseId(courseId, sortBy, actualSortOrder, offset, size)
        val totalCount = courseRepository.countBatchesByCourseId(courseId)
        
        return PaginatedResponse(
            data = batches.map { mapToBatchResponse(it) },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }

    @Transactional(readOnly = true)
    fun searchCourseBatches(
        courseId: UUID,
        query: String,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<BatchResponse> {
        if (!courseRepository.existsById(courseId)) {
            throw NotFoundException("Course with id $courseId not found")
        }

        val actualSortOrder = sortOrder.uppercase()
        val offset = page * size
        
        val batches = courseRepository.searchBatchesByCourseId(courseId, query, sortBy, actualSortOrder, offset, size)
        val totalCount = courseRepository.countSearchBatchesByCourseId(courseId, query)
        
        return PaginatedResponse(
            data = batches.map { mapToBatchResponse(it) },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }

    @Transactional(readOnly = true)
    fun searchCourseStudents(
        courseId: UUID,
        query: String,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<UserResponse> {
        if (!courseRepository.existsById(courseId)) {
            throw NotFoundException("Course with id $courseId not found")
        }

        val actualSortOrder = sortOrder.uppercase()
        val offset = page * size

        val students = courseRepository.searchStudentsByCourseId(courseId, query, sortBy, actualSortOrder, offset, size)
        val totalCount = courseRepository.countSearchStudentsByCourseId(courseId, query)
        
        return PaginatedResponse(
            data = students.map { mapToUserResponse(it) },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }

    @Cacheable(value = [CacheConfig.COURSES_ACTIVE_CACHE], key = "'faculty-' + #facultyId")
    @Transactional(readOnly = true)
    fun getFacultyActiveCourses(facultyId: String): List<CourseResponse> {
        val ids = courseRepository.findActiveCourseIdsByInstructor(facultyId, "name", "ASC", 0, 10000)
        
        if (ids.isEmpty()) {
            return emptyList()
        }
        
        val coursesData = courseRepository.findCourseListData(ids)
        val coursesByIdMap = coursesData.associateBy { UUID.fromString(it["id"].toString()) }
        
        return ids.mapNotNull { id ->
            coursesByIdMap[id]?.let { data ->
                CourseResponse(
                    id = UUID.fromString(data["id"].toString()),
                    name = data["name"].toString(),
                    code = data["code"]?.toString() ?: "",
                    description = data["description"]?.toString() ?: ""
                )
            }
        }
    }

    private fun mapToCourseResponse(data: Map<String, Any>): CourseResponse {
        return CourseResponse(
            id = UUID.fromString(data["id"].toString()),
            name = data["name"].toString(),
            code = data["code"]?.toString() ?: "",
            description = data["description"]?.toString() ?: ""
        )
    }

    private fun mapToUserResponse(data: Map<String, Any>): UserResponse {
        return UserResponse(
            id = data["id"].toString(),
            name = data["name"].toString(),
            email = data["email"].toString(),
            profileId = data["profile_id"]?.toString(),
            image = data["image"]?.toString(),
            role = data["role"].toString(),
            phoneNumber = data["phone_number"]?.toString(),
            isActive = data["is_active"] as? Boolean ?: true,
            createdAt = data["created_at"] as? Timestamp ?: Timestamp(System.currentTimeMillis())
        )
    }

    private fun mapToBatchResponse(data: Map<String, Any>): BatchResponse {
        return BatchResponse(
            id = UUID.fromString(data["id"].toString()),
            name = data["name"].toString(),
            graduationYear = Year.of((data["graduation_year"] as Number).toInt()),
            section = data["section"].toString(),
            isActive = true,
            department = null
        )
    }
}

fun Course.toCourseResponse(): CourseResponse {
    return CourseResponse(
        id = this.id!!,
        name = this.name,
        code = this.code,
        description = this.description
    )
}
