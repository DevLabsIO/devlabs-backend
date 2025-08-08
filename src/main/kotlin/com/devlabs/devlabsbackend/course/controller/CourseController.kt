package com.devlabs.devlabsbackend.course.controller

import com.devlabs.devlabsbackend.batch.domain.dto.BatchResponse
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.course.domain.DTO.CourseResponse
import com.devlabs.devlabsbackend.course.service.CourseService
import com.devlabs.devlabsbackend.security.utils.SecurityUtils
import com.devlabs.devlabsbackend.user.domain.DTO.UserResponse
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/course")
class CourseController(
    private val courseService: CourseService,
    private val userRepository: UserRepository
) {

    @GetMapping("/{courseId}")
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
    fun addBatchToCourse(@PathVariable courseId: UUID, @RequestBody batchIds: List<UUID>): ResponseEntity<Any> {
        return try {
            courseService.addBatchesToCourse(courseId, batchIds)
            ResponseEntity.ok().build()
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to add batches to course: ${e.message}"))
        }
    }

    @DeleteMapping("/{courseId}/batches/{batchId}")
    fun removeBatchFromCourse(@PathVariable courseId: UUID, @PathVariable batchId: UUID): ResponseEntity<Any> {
        return try {
            courseService.removeBatchesFromCourse(courseId, listOf(batchId))
            ResponseEntity.ok().build()
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to remove batch from course: ${e.message}"))
        }
    }

    @PostMapping("/{courseId}/students")
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
                .body(mapOf("message" to "Failed to assign students to course: ${e.message}"))
        }
    }

    @DeleteMapping("/{courseId}/students/{studentId}")
    fun removeStudentFromCourse(@PathVariable courseId: UUID, @PathVariable studentId: String): ResponseEntity<Any> {
        return try {
            courseService.removeStudents(courseId, listOf(studentId))
            ResponseEntity.ok().build()
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to remove student from course: ${e.message}"))
        }
    }

    @GetMapping("/{courseId}/instructors")
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
                .body(mapOf("message" to "Failed to assign instructors to course: ${e.message}"))
        }
    }


    @DeleteMapping("/{courseId}/instructors/{instructorId}")
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
                .body(mapOf("message" to "Failed to remove instructor from course: ${e.message}"))
        }
    }

    @GetMapping("/active")
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
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error retrieving courses: ${e.message}"))
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("message" to "Unauthorized access - $userGroup role cannot access course information"))

    }


    @GetMapping("/{userId}/active-courses")
    fun getUserActiveCourses(@PathVariable userId: String): ResponseEntity<Any> {
        return try {
            val courses = courseService.getActiveCoursesForUser(userId)
            ResponseEntity.ok(courses)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve user's active courses: ${e.message}"))
        }
    }

    @GetMapping("/student/courses-with-scores")
    fun getStudentCoursesWithScores(): ResponseEntity<Any> {
        val studentId = SecurityUtils.getCurrentUserId()?:throw IllegalArgumentException("User not authenticated")
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
                .body(mapOf("error" to "Failed to retrieve student's courses with scores: ${e.message}"))
        }
    }

    @GetMapping("/student/{studentId}/course/{courseId}/review")
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
                .body(mapOf("error" to "Failed to retrieve performance chart data: ${e.message}"))
        }
    }

    @PostMapping("/my-courses")
    fun getMyActiveCourses(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "name") sort_by: String,
        @RequestParam(defaultValue = "asc") sort_order: String
    ): ResponseEntity<Any> {
        return try {

            val userId = SecurityUtils.getCurrentUserId()?: throw IllegalArgumentException("User not authenticated")
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
                .body(mapOf("error" to "Failed to get courses: ${e.message}"))
        }
    }

    @PostMapping("/my-courses/search")
    fun searchMyActiveCourses(
        @RequestBody request: Map<String, Any>,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "name") sort_by: String,
        @RequestParam(defaultValue = "asc") sort_order: String
    ): ResponseEntity<Any> {
        return try {
            val userId = SecurityUtils.getCurrentUserId()?: throw IllegalArgumentException("User not authenticated")
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
                .body(mapOf("error" to "Failed to search courses: ${e.message}"))
        }
    }

    @GetMapping("/{courseId}/students")
    fun getCourseStudents(
        @PathVariable courseId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "name") sort_by: String,
        @RequestParam(defaultValue = "asc") sort_order: String
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
    fun getCourseBatches(
        @PathVariable courseId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "name") sort_by: String,
        @RequestParam(defaultValue = "asc") sort_order: String
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
    fun searchCourseBatches(
        @PathVariable courseId: UUID,
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "name") sort_by: String,
        @RequestParam(defaultValue = "asc") sort_order: String
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
    fun searchCourseStudents(
        @PathVariable courseId: UUID,
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "name") sort_by: String,
        @RequestParam(defaultValue = "asc") sort_order: String
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