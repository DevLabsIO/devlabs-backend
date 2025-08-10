package com.devlabs.devlabsbackend.course.service

import com.devlabs.devlabsbackend.batch.domain.dto.BatchResponse
import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.batch.service.toBatchResponse
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.course.domain.DTO.CoursePerformanceChartResponse
import com.devlabs.devlabsbackend.course.domain.DTO.CourseResponse
import com.devlabs.devlabsbackend.course.domain.DTO.StudentCourseWithScoresResponse
import com.devlabs.devlabsbackend.course.repository.CourseRepository
import com.devlabs.devlabsbackend.individualscore.repository.IndividualScoreRepository
import com.devlabs.devlabsbackend.review.service.ReviewPublicationHelper
import com.devlabs.devlabsbackend.user.domain.DTO.UserResponse
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import com.devlabs.devlabsbackend.user.repository.UserRepository
import com.devlabs.devlabsbackend.user.service.toUserResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val batchRepository: BatchRepository,
    private val individualScoreRepository: IndividualScoreRepository,
    private val reviewPublicationHelper: ReviewPublicationHelper
) {

    @Transactional(readOnly = true)
    fun getCourseById(courseId: UUID): CourseResponse {
        val course = courseRepository.findById(courseId).orElseThrow {
            NotFoundException("Course with id $courseId not found")
        }
        return course.toCourseResponse()
    }

    @Transactional
    fun addBatchesToCourse(courseId: UUID, batchId: List<UUID>){
        val course = courseRepository.findByIdWithBatches(courseId) ?: throw NotFoundException("Could not find course with id $courseId")
        val batches = batchRepository.findAllById(batchId)
        course.batches.addAll(batches)
        courseRepository.save(course)
    }

    @Transactional
    fun removeBatchesFromCourse(courseId: UUID, batchId: List<UUID>){
        val course = courseRepository.findByIdWithBatches(courseId) ?: throw NotFoundException("Could not find course with id $courseId")
        val batches = batchRepository.findAllById(batchId)
        course.batches.removeAll(batches)
        courseRepository.save(course)
    }

    @Transactional
    fun assignStudents(courseId: UUID, studentId:List<String>){
        val course = courseRepository.findByIdWithStudents(courseId) ?: throw NotFoundException("Could not find course with id $courseId")
        val users = userRepository.findAllById(studentId)
        course.students.addAll(users)
        courseRepository.save(course)
    }

    @Transactional
    fun removeStudents(courseId: UUID, studentId: List<String>){
        val course = courseRepository.findByIdWithStudents(courseId) ?: throw NotFoundException("Could not find course with id $courseId")
        val users = userRepository.findAllById(studentId)
        course.students.removeAll(users)
        courseRepository.save(course)
    }

    @Transactional(readOnly = true)
    fun getCourseInstructors(
        courseId: UUID
    ): List<UserResponse> {
        val course = courseRepository.findByIdWithInstructors(courseId) ?: throw NotFoundException("Course with id $courseId not found")
        return course.instructors.map { it.toUserResponse() }
    }

    @Transactional
    fun assignInstructors(courseId: UUID, instructorId:List<String>){
        val course = courseRepository.findByIdWithInstructors(courseId) ?: throw NotFoundException("Could not find course with id $courseId")
        val users = userRepository.findAllById(instructorId)
        course.instructors.addAll(users)
        courseRepository.save(course)
    }

    @Transactional
    fun removeInstructors(courseId: UUID, instructorId:List<String>){
        val course = courseRepository.findByIdWithInstructors(courseId) ?: throw NotFoundException("Could not find course with id $courseId")
        val users = userRepository.findAllById(instructorId)
        course.instructors.removeAll(users)
        courseRepository.save(course)
    }

    @Transactional
    fun getAllActiveCourses(): List<CourseResponse> {
        return courseRepository.findCoursesByActiveSemesters().map { it.toCourseResponse() }
    }


    @Transactional(readOnly = true)
    fun getActiveCoursesForUser(userId: String): List<CourseResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }
        return when (user.role) {
            Role.STUDENT -> {
                val directCourses = courseRepository.findCoursesByActiveSemestersAndStudent(user)
                if (directCourses.isEmpty()) {
                    courseRepository.findCoursesByActiveSemestersAndStudentThroughBatch(user).map { it.toCourseResponse() }
                } else {
                    directCourses.map { it.toCourseResponse() }
                }
            }
            Role.FACULTY -> {
                courseRepository.findCoursesByActiveSemestersAndInstructor(user).map { it.toCourseResponse() }
            }
            Role.ADMIN, Role.MANAGER -> {
                courseRepository.findCoursesByActiveSemesters().map { it.toCourseResponse() }
            }
        }
    }

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
        val isDirectlyEnrolled = course.students.contains(student)
        val batchEnrolledCourses = courseRepository.findCoursesByActiveSemestersAndStudentThroughBatch(student)
        val isEnrolledThroughBatch = batchEnrolledCourses.contains(course)

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
                showResult = true, // Always true since we're only showing published reviews
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
        val direction = if (sortOrder.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC
        val sort = Sort.by(direction, sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val coursePage = when (currentUser.role) {
            Role.MANAGER -> {
                courseRepository.findCoursesByActiveSemesters(pageable)
            }
            Role.FACULTY -> {
                courseRepository.findCoursesByActiveSemestersAndInstructor(currentUser, pageable)
            }
            else -> {
                throw IllegalArgumentException("Access denied. Only MANAGER and FACULTY roles can access this endpoint.")
            }
        }

        return PaginatedResponse(
            data = coursePage.content.map { it.toCourseResponse() },
            pagination = PaginationInfo(
                current_page = coursePage.number,
                per_page = coursePage.size,
                total_pages = coursePage.totalPages,
                total_count = coursePage.totalElements.toInt()
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
        val direction = if (sortOrder.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC
        val sort = Sort.by(direction, sortBy)
        val pageable = PageRequest.of(page, size, sort)

        val coursePage = when (currentUser.role) {
            Role.MANAGER -> {
                courseRepository.searchCoursesByActiveSemesters(query, pageable)
            }
            Role.FACULTY -> {
                courseRepository.searchCoursesByActiveSemestersAndInstructor(currentUser, query, pageable)
            }
            else -> {
                throw IllegalArgumentException("Access denied. Only MANAGER and FACULTY roles can access this endpoint.")
            }
        }

        return PaginatedResponse(
            data = coursePage.content.map { it.toCourseResponse() },
            pagination = PaginationInfo(
                current_page = coursePage.number,
                per_page = coursePage.size,
                total_pages = coursePage.totalPages,
                total_count = coursePage.totalElements.toInt()
            )
        )
    }

    @Transactional(readOnly = true)
    fun getCourseStudents(
        courseId: UUID,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<UserResponse> {
        val course = courseRepository.findByIdWithStudents(courseId) ?: throw NotFoundException("Course with id $courseId not found")

        val studentUsers = course.students.filter { it.role == Role.STUDENT }

        val direction = if (sortOrder.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC

        val sortedStudents = when (sortBy.lowercase()) {
            "name" -> if (direction == Sort.Direction.ASC)
                studentUsers.sortedBy { it.name }
            else
                studentUsers.sortedByDescending { it.name }
            "email" -> if (direction == Sort.Direction.ASC)
                studentUsers.sortedBy { it.email }
            else
                studentUsers.sortedByDescending { it.email }
            "createdat" -> if (direction == Sort.Direction.ASC)
                studentUsers.sortedBy { it.createdAt }
            else
                studentUsers.sortedByDescending { it.createdAt }
            else -> studentUsers.sortedBy { it.name }
        }
        val totalElements = sortedStudents.size
        val totalPages = (totalElements + size - 1) / size
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, totalElements)

        val pagedStudents = if (startIndex < totalElements) {
            sortedStudents.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PaginatedResponse(
            data = pagedStudents.map { it.toUserResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = totalPages,
                total_count = totalElements
            )
        )
    }

    @Transactional(readOnly = true)
    fun getCourseBatches(
        courseId: UUID,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<BatchResponse> {
        val course = courseRepository.findByIdWithBatches(courseId) ?: throw NotFoundException("Course with id $courseId not found")

        val direction = if (sortOrder.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC

        val sortedBatches = when (sortBy.lowercase()) {
            "name" -> if (direction == Sort.Direction.ASC)
                course.batches.sortedBy { it.name }
            else
                course.batches.sortedByDescending { it.name }
            "graduationyear" -> if (direction == Sort.Direction.ASC)
                course.batches.sortedBy { it.graduationYear }
            else
                course.batches.sortedByDescending { it.graduationYear }
            "section" -> if (direction == Sort.Direction.ASC)
                course.batches.sortedBy { it.section }
            else
                course.batches.sortedByDescending { it.section }
            else -> course.batches.sortedBy { it.name }
        }

        val totalElements = sortedBatches.size
        val totalPages = (totalElements + size - 1) / size
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, totalElements)

        val pagedBatches = if (startIndex < totalElements) {
            sortedBatches.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PaginatedResponse(
            data = pagedBatches.map { it.toBatchResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = totalPages,
                total_count = totalElements
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
        val course = courseRepository.findByIdWithBatches(courseId) ?: throw NotFoundException("Course with id $courseId not found")

        val filteredBatches = course.batches.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.section.contains(query, ignoreCase = true) ||
                    it.graduationYear.toString().contains(query, ignoreCase = true)
        }

        val direction = if (sortOrder.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC
        val sortedBatches = when (sortBy.lowercase()) {
            "name" -> if (direction == Sort.Direction.ASC)
                filteredBatches.sortedBy { it.name }
            else
                filteredBatches.sortedByDescending { it.name }
            "graduationyear" -> if (direction == Sort.Direction.ASC)
                filteredBatches.sortedBy { it.graduationYear }
            else
                filteredBatches.sortedByDescending { it.graduationYear }
            "section" -> if (direction == Sort.Direction.ASC)
                filteredBatches.sortedBy { it.section }
            else
                filteredBatches.sortedByDescending { it.section }
            else -> filteredBatches.sortedBy { it.name }
        }
        val totalElements = sortedBatches.size
        val totalPages = (totalElements + size - 1) / size
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, totalElements)

        val pagedBatches = if (startIndex < totalElements) {
            sortedBatches.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PaginatedResponse(
            data = pagedBatches.map { it.toBatchResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = totalPages,
                total_count = totalElements
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
        val course = courseRepository.findByIdWithStudents(courseId) ?: throw NotFoundException("Course with id $courseId not found")

        val filteredStudents = course.students.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true) ||
                    it.profileId?.contains(query, ignoreCase = true) == true
        }

        val direction = if (sortOrder.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC

        val sortedStudents = when (sortBy.lowercase()) {
            "name" -> if (direction == Sort.Direction.ASC)
                filteredStudents.sortedBy { it.name }
            else
                filteredStudents.sortedByDescending { it.name }
            "email" -> if (direction == Sort.Direction.ASC)
                filteredStudents.sortedBy { it.email }
            else
                filteredStudents.sortedByDescending { it.email }
            "createdat" -> if (direction == Sort.Direction.ASC)
                filteredStudents.sortedBy { it.createdAt }
            else
                filteredStudents.sortedByDescending { it.createdAt }
            else -> filteredStudents.sortedBy { it.name } // Default sort by name
        }

        val totalElements = sortedStudents.size
        val totalPages = (totalElements + size - 1) / size 
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, totalElements)

        val pagedStudents = if (startIndex < totalElements) {
            sortedStudents.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        return PaginatedResponse(
            data = pagedStudents.map { it.toUserResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = totalPages,
                total_count = totalElements
            )
        )
    }

    fun getFacultyActiveCourses(facultyId: String): List<CourseResponse> {
        val faculty = userRepository.findById(facultyId).orElseThrow {
            NotFoundException("Faculty with id $facultyId not found")
        }
        if (faculty.role != Role.FACULTY) {
            throw IllegalArgumentException("User is not a faculty member")
        }
        return courseRepository.findCoursesByActiveSemestersAndInstructor(faculty).map { it.toCourseResponse() }
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
