package com.devlabs.devlabsbackend.course.controller

import com.devlabs.devlabsbackend.batch.domain.dto.BatchResponse
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.course.domain.dto.CourseResponse
import com.devlabs.devlabsbackend.course.service.CourseService
import com.devlabs.devlabsbackend.security.utils.SecurityUtils
import com.devlabs.devlabsbackend.user.domain.dto.UserResponse
import com.devlabs.devlabsbackend.user.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/course")
@Tag(name = "Course", description = "Course management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
class CourseController(
    private val courseService: CourseService,
    private val userRepository: UserRepository
) {

    @GetMapping("/{courseId}")
    @Operation(summary = "Get course by ID", description = "Retrieve course information by ID")
    fun getCourseById(@PathVariable courseId: UUID): ResponseEntity<CourseResponse> {
        return try {
            val course = courseService.getCourseById(courseId)
            ResponseEntity.ok(course)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }

    @PutMapping("/{courseId}/addBatch")
    @Operation(summary = "Add batches to course", description = "Add batches to a course")
    fun addBatchToCourse(@PathVariable courseId: UUID, @RequestBody batchIds: List<UUID>): ResponseEntity<Any> {
        return try {
            courseService.addBatchesToCourse(courseId, batchIds)
            ResponseEntity.ok().build()
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to add batches to course: \${e.message}"))
        }
    }

    @DeleteMapping("/{courseId}/batches/{batchId}")
    @Operation(summary = "Remove batch from course", description = "Remove a batch from a course")
    fun removeBatchFromCourse(@PathVariable courseId: UUID, @PathVariable batchId: UUID): ResponseEntity<Any> {
        return try {
            courseService.removeBatchesFromCourse(courseId, listOf(batchId))
            ResponseEntity.ok().build()
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to remove batch from course: \${e.message}"))
        }
    }

    @PostMapping("/{courseId}/students")
    @Operation(summary = "Assign students to course", description = "Assign students to a course")
    fun assignStudentsToCoursePost(
        @PathVariable courseId: UUID,
        @RequestBody requestBody: Map<String, List<String>>
    ): ResponseEntity<Any> {
        return try {
            val studentIds = requestBody["studentIds"] ?: emptyList()
            courseService.assignStudents(courseId, studentIds)
            ResponseEntity.ok().build()
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to assign students to course: \${e.message}"))
        }
    }

    @DeleteMapping("/{courseId}/students/{studentId}")
    @Operation(summary = "Remove student from course", description = "Remove a student from a course")
    fun removeStudentFromCourse(@PathVariable courseId: UUID, @PathVariable studentId: String): ResponseEntity<Any> {
        return try {
            courseService.removeStudents(courseId, listOf(studentId))
            ResponseEntity.ok().build()
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to remove student from course: \${e.message}"))
        }
    }

    @GetMapping("/{courseId}/instructors")
    @Operation(summary = "Get course instructors", description = "Get all instructors for a course")
    fun getCourseInstructors(
        @PathVariable courseId: UUID
    ): ResponseEntity<List<UserResponse>> {
        return try {
            val instructors = courseService.getCourseInstructors(courseId)
            ResponseEntity.ok(instructors)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }

    @PostMapping("/{courseId}/instructors")
    @Operation(summary = "Assign instructors to course", description = "Assign instructors to a course")
    fun assignInstructorsToCoursePost(
        @PathVariable courseId: UUID,
        @RequestBody requestBody: Map<String, List<String>>
    ): ResponseEntity<Any> {
        return try {
            val instructorIds = requestBody["instructorIds"] ?: emptyList()
            courseService.assignInstructors(courseId, instructorIds)
            ResponseEntity.ok().build()
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to assign instructors to course: \${e.message}"))
        }
    }

    @DeleteMapping("/{courseId}/instructors/{instructorId}")
    @Operation(summary = "Remove instructor from course", description = "Remove an instructor from a course")
    fun removeInstructorFromCourse(
        @PathVariable courseId: UUID,
        @PathVariable instructorId: String
    ): ResponseEntity<Any> {
        return try {
            courseService.removeInstructors(courseId, listOf(instructorId))
            ResponseEntity.ok().build()
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to remove instructor from course: \${e.message}"))
        }
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active courses", description = "Get all active courses based on user role")
    fun getAllActiveCourses(): ResponseEntity<Any> {
        val rawUserGroup = SecurityUtils.getCurrentJwtClaim("groups")
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to "User not authenticated"))
        val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")
        try {
            if (userGroup.equals("admin", ignoreCase = true) || userGroup.equals("manager", ignoreCase = true)) {
                val courses = courseService.getAllActiveCourses()
                return ResponseEntity.ok(courses)
            }
            if (userGroup.equals("faculty", ignoreCase = true)) {
                val currentUserId = SecurityUtils.getCurrentUserId()
                    ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(mapOf("message" to "User not authenticated"))
                val courses = courseService.getFacultyActiveCourses(currentUserId)
                return ResponseEntity.ok(courses)
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to "Unauthorized access - $userGroup role cannot access course information"))
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error retrieving courses: ${e.message}"))
        }
    }

    @GetMapping("/{userId}/active-courses")
    @Operation(summary = "Get user active courses", description = "Get all active courses for a specific user")
    fun getUserActiveCourses(@PathVariable userId: String): ResponseEntity<Any> {
        return try {
            val courses = courseService.getActiveCoursesForUser(userId)
            ResponseEntity.ok(courses)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve user's active courses: \${e.message}"))
        }
    }

    @GetMapping("/student/courses-with-scores")
    @Operation(summary = "Get student courses with scores", description = "Get student's courses with their average scores")
    fun getStudentCoursesWithScores(): ResponseEntity<Any> {
        val studentId = SecurityUtils.getCurrentUserId() ?: throw IllegalArgumentException("User not authenticated")
        return try {
            val coursesWithScores = courseService.getStudentActiveCoursesWithScores(studentId)
            ResponseEntity.ok(coursesWithScores)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve student's courses with scores: \${e.message}"))
        }
    }

    @GetMapping("/student/{studentId}/course/{courseId}/review")
    @Operation(summary = "Get student course performance chart", description = "Get student's performance chart for a specific course")
    fun getStudentCoursePerformanceChart(
        @PathVariable studentId: String,
        @PathVariable courseId: UUID
    ): ResponseEntity<Any> {
        return try {
            val performanceData = courseService.getStudentCoursePerformanceChart(studentId, courseId)
            ResponseEntity.ok(performanceData)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve performance chart data: \${e.message}"))
        }
    }

    @PostMapping("/my-courses")
    @Operation(summary = "Get my courses", description = "Get paginated list of courses for current user")
    fun getMyActiveCourses(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sort_by: String,
        @RequestParam(defaultValue = "desc") sort_order: String
    ): ResponseEntity<Any> {
        return try {
            val userId = SecurityUtils.getCurrentUserId() ?: throw IllegalArgumentException("User not authenticated")
            val currentUser = userRepository.findById(userId).orElse(null)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "User not found"))
            val courses = courseService.getCoursesForCurrentUser(currentUser, page, size, sort_by, sort_order)
            ResponseEntity.ok(courses)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get courses: \${e.message}"))
        }
    }

    @PostMapping("/my-courses/search")
    @Operation(summary = "Search my courses", description = "Search courses for current user with pagination")
    fun searchMyActiveCourses(
        @RequestBody request: Map<String, Any>,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sort_by: String,
        @RequestParam(defaultValue = "desc") sort_order: String
    ): ResponseEntity<Any> {
        return try {
            val userId = SecurityUtils.getCurrentUserId() ?: throw IllegalArgumentException("User not authenticated")
            val query = request["query"]?.toString()
                ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(mapOf("error" to "Query parameter is required"))

            val currentUser = userRepository.findById(userId).orElse(null)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to "User not found"))

            val courses = courseService.searchCoursesForCurrentUser(currentUser, query, page, size, sort_by, sort_order)
            ResponseEntity.ok(courses)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to search courses: \${e.message}"))
        }
    }

    @GetMapping("/{courseId}/students")
    @Operation(summary = "Get course students", description = "Get paginated list of students in a course")
    fun getCourseStudents(
        @PathVariable courseId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sort_by: String,
        @RequestParam(defaultValue = "desc") sort_order: String
    ): ResponseEntity<PaginatedResponse<UserResponse>> {
        return try {
            val students = courseService.getCourseStudents(courseId, page, size, sort_by, sort_order)
            ResponseEntity.ok(students)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }

    @GetMapping("/{courseId}/batches")
    @Operation(summary = "Get course batches", description = "Get paginated list of batches in a course")
    fun getCourseBatches(
        @PathVariable courseId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sort_by: String,
        @RequestParam(defaultValue = "desc") sort_order: String
    ): ResponseEntity<PaginatedResponse<BatchResponse>> {
        return try {
            val batches = courseService.getCourseBatches(courseId, page, size, sort_by, sort_order)
            ResponseEntity.ok(batches)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }

    @GetMapping("/{courseId}/batches/search")
    @Operation(summary = "Search course batches", description = "Search batches in a course with pagination")
    fun searchCourseBatches(
        @PathVariable courseId: UUID,
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sort_by: String,
        @RequestParam(defaultValue = "desc") sort_order: String
    ): ResponseEntity<PaginatedResponse<BatchResponse>> {
        return try {
            val batches = courseService.searchCourseBatches(courseId, query, page, size, sort_by, sort_order)
            ResponseEntity.ok(batches)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }

    @GetMapping("/{courseId}/students/search")
    @Operation(summary = "Search course students", description = "Search students in a course with pagination")
    fun searchCourseStudents(
        @PathVariable courseId: UUID,
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sort_by: String,
        @RequestParam(defaultValue = "desc") sort_order: String
    ): ResponseEntity<PaginatedResponse<UserResponse>> {
        return try {
            val students = courseService.searchCourseStudents(courseId, query, page, size, sort_by, sort_order)
            ResponseEntity.ok(students)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(null)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }
}
