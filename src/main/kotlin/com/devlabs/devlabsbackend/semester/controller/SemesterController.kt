package com.devlabs.devlabsbackend.semester.controller

import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.course.domain.dto.CourseResponse
import com.devlabs.devlabsbackend.semester.domain.dto.*
import com.devlabs.devlabsbackend.semester.service.SemesterService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/semester")
@Tag(name = "Semester", description = "Semester management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
class SemesterController(private val semesterService: SemesterService) {
    
    @GetMapping
    fun getAllSemesters(
        @RequestParam(required = false) isActive: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortOrder: String
    ): ResponseEntity<PaginatedResponse<SemesterResponse>> {
        return ResponseEntity.ok(semesterService.getAllSemestersPaginated(isActive, page, size, sortBy, sortOrder))
    }
    
    @GetMapping("/search")
    fun searchSemesters(
        @RequestParam query: String,
        @RequestParam(required = false) isActive: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortOrder: String
    ): ResponseEntity<PaginatedResponse<SemesterResponse>> {
        return ResponseEntity.ok(semesterService.searchSemesterPaginated(query, isActive, page, size, sortBy, sortOrder))
    }
    
    @GetMapping("/active")
    fun getActiveSemesters(): ResponseEntity<Any> {
        return try {
            val rawUserGroup = com.devlabs.devlabsbackend.security.utils.SecurityUtils.getCurrentJwtClaim("groups")
                ?: return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(mapOf("message" to "User not authenticated"))
            
            val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")
            
            val semesters = when {
                userGroup.equals("admin", ignoreCase = true) || userGroup.equals("manager", ignoreCase = true) -> {
                    semesterService.getAllActiveSemesters()
                }
                userGroup.equals("faculty", ignoreCase = true) -> {
                    val currentUserId = com.devlabs.devlabsbackend.security.utils.SecurityUtils.getCurrentUserId()
                        ?: return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                            .body(mapOf("message" to "User not authenticated"))
                    semesterService.getAllActiveSemesters(currentUserId)
                }
                else -> {
                    return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                        .body(mapOf("message" to "Unauthorized access - $userGroup role cannot access semester information"))
                }
            }
            
            ResponseEntity.ok(semesters)
        } catch (e: Exception) {
            ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Error retrieving semesters: ${e.message}"))
        }
    }
    
    @GetMapping("/{id}")
    fun getSemesterById(@PathVariable id: UUID): ResponseEntity<SemesterResponse> {
        return ResponseEntity.ok(semesterService.getSemesterById(id))
    }
    
    @PostMapping
    fun createSemester(@RequestBody request: CreateSemesterRequest): ResponseEntity<SemesterResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(semesterService.createSemester(request))
    }
    
    @PutMapping("/{id}")
    fun updateSemester(
        @PathVariable id: UUID,
        @RequestBody request: UpdateSemesterDTO
    ): ResponseEntity<SemesterResponse> {
        return ResponseEntity.ok(semesterService.updateSemester(id, request))
    }
    
    @DeleteMapping("/{id}")
    fun deleteSemester(@PathVariable id: UUID): ResponseEntity<Void> {
        semesterService.deleteSemester(id)
        return ResponseEntity.noContent().build()
    }
    
    @GetMapping("/{semesterId}/courses")
    fun getCoursesBySemester(@PathVariable semesterId: UUID): ResponseEntity<List<CourseResponse>> {
        return ResponseEntity.ok(semesterService.getCoursesBySemesterId(semesterId))
    }
    
    @PostMapping("/{id}/courses")
    fun createCourseForSemester(
        @PathVariable id: UUID,
        @RequestBody courseRequest: com.devlabs.devlabsbackend.course.domain.dto.CreateCourseRequest
    ): ResponseEntity<CourseResponse> {
        return try {
            val course = semesterService.createCourseForSemester(id, courseRequest)
            ResponseEntity(course, HttpStatus.CREATED)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }

    @PutMapping("/{semesterId}/courses/{courseId}")
    fun updateCourseForSemester(
        @PathVariable semesterId: UUID,
        @PathVariable courseId: UUID,
        @RequestBody courseRequest: com.devlabs.devlabsbackend.course.domain.dto.UpdateCourseRequest
    ): ResponseEntity<CourseResponse> {
        return try {
            val course = semesterService.updateCourseForSemester(semesterId, courseId, courseRequest)
            ResponseEntity(course, HttpStatus.OK)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(null)
        } catch (e: com.devlabs.devlabsbackend.core.exception.NotFoundException) {
            ResponseEntity.notFound().build()
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
        } catch (e: com.devlabs.devlabsbackend.core.exception.NotFoundException) {
            ResponseEntity.notFound().build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null)
        }
    }
}
