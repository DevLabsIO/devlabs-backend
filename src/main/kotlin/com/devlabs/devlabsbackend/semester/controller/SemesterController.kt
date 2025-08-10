package com.devlabs.devlabsbackend.semester.controller

import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.course.domain.DTO.CourseResponse
import com.devlabs.devlabsbackend.course.domain.DTO.CreateCourseRequest
import com.devlabs.devlabsbackend.security.utils.SecurityUtils
import com.devlabs.devlabsbackend.semester.domain.DTO.SemesterResponse
import com.devlabs.devlabsbackend.semester.domain.DTO.UpdateSemesterDTO
import com.devlabs.devlabsbackend.semester.service.SemesterService
import com.devlabs.devlabsbackend.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/semester")
class SemesterController(val semesterService: SemesterService, val userService: UserService) {

    @GetMapping
    fun getAllSemesters(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "name") sort_by: String,
        @RequestParam(defaultValue = "asc") sort_order: String
    ): ResponseEntity<PaginatedResponse<SemesterResponse>> {
        return ResponseEntity.ok(
            semesterService.getAllSemestersPaginated(page, size, sort_by, sort_order)
        )
    }

    @GetMapping("/search")
    fun search(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "name") sort_by: String,
        @RequestParam(defaultValue = "asc") sort_order: String
    ): ResponseEntity<PaginatedResponse<SemesterResponse>> {
        return ResponseEntity.ok(
            semesterService.searchSemesterPaginated(query, page, size, sort_by, sort_order)
        )
    }

    @PostMapping
    fun createSemester(@RequestBody request: com.devlabs.devlabsbackend.semester.domain.DTO.CreateSemesterRequest): ResponseEntity<SemesterResponse> {
        return try {
            val semester = semesterService.createSemester(request)
            ResponseEntity(semester, HttpStatus.CREATED)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }

    @GetMapping("/active")
    fun getAllActiveSemesters(): ResponseEntity<Any> {
        val rawUserGroup = SecurityUtils.getCurrentJwtClaim("groups")
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to "User not authenticated"))

        val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")

        return try {
            when {
                userGroup.equals("admin", ignoreCase = true)  || userGroup.equals("manager", ignoreCase = true) -> {
                    val allSemesters = semesterService.getAllActiveSemesters()
                    ResponseEntity.ok(allSemesters)
                }

                userGroup.equals("faculty", ignoreCase = true) -> {
                    val currentUserId = SecurityUtils.getCurrentUserId()
                        ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to "User not authenticated"))

                    val facultySemesters = semesterService.getFacultyAssignedSemesters(currentUserId)
                    ResponseEntity.ok(facultySemesters)
                }

                else -> {
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(mapOf("message" to "Unauthorized access - $userGroup role cannot access semester information"))
                }
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error retrieving semesters: ${e.message}"))
        }
    }

    @PutMapping("/{semesterId}")
    fun updateSemester(
        @PathVariable semesterId: UUID,
        @RequestBody request: UpdateSemesterDTO
    ): ResponseEntity<SemesterResponse> {
        return try {
            val updatedSemester = semesterService.updateSemester(semesterId, request)
            ResponseEntity.ok(updatedSemester)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(null)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }
    @DeleteMapping("/{semesterId}")
    fun deleteSemester(@PathVariable semesterId: UUID): ResponseEntity<Void> {
        return try {
            semesterService.deleteSemester(semesterId)
            ResponseEntity.noContent().build()
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }


    @GetMapping("/{id}")
    fun getSemesterById(@PathVariable id: UUID): ResponseEntity<SemesterResponse> {
        return ResponseEntity(
            semesterService.getSemesterById(id),
            HttpStatus.OK
        )
    }


    @GetMapping("/{id}/courses")
    fun getCoursesBySemesterId(@PathVariable id: UUID): ResponseEntity<List<CourseResponse>> {
        return ResponseEntity(
            semesterService.getCoursesBySemesterId(id),
            HttpStatus.OK
        )
    }

    @PostMapping("/{id}/courses")
    fun createCourseForSemester(
        @PathVariable id: UUID,
        @RequestBody courseRequest: CreateCourseRequest
    ): ResponseEntity<CourseResponse> {
        return try {
            val course = semesterService.createCourseForSemester(id, courseRequest)
            ResponseEntity(course, HttpStatus.CREATED)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }

    @DeleteMapping("/{semesterId}/courses/{courseId}")
    fun deleteCourseFromSemester(
        @PathVariable semesterId: UUID,
        @PathVariable courseId: UUID
    ): ResponseEntity<CourseResponse> {
        return try {
            val course = semesterService.deleteCourseFromSemester(semesterId, courseId)
            ResponseEntity(course, HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(null)
        } catch (e: NotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }
}