package com.devlabs.devlabsbackend.project.controller

import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.project.domain.dto.CreateProjectRequest
import com.devlabs.devlabsbackend.project.domain.dto.UpdateProjectRequest
import com.devlabs.devlabsbackend.project.domain.dto.UserIdRequest
import com.devlabs.devlabsbackend.project.domain.dto.toProjectResponse
import com.devlabs.devlabsbackend.project.service.ProjectService
import com.devlabs.devlabsbackend.project.service.ProjectStatusService
import com.devlabs.devlabsbackend.review.service.ReviewQueryService
import com.devlabs.devlabsbackend.security.utils.SecurityUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/projects")
class ProjectController(
    private val projectService: ProjectService,
    private val projectStatusService: ProjectStatusService,
    private val reviewQueryService: ReviewQueryService
) {

    @PostMapping
    fun createProject(@RequestBody request: CreateProjectRequest): ResponseEntity<Any> {
        return try {
            val project = projectService.createProject(request)
            ResponseEntity.status(HttpStatus.CREATED).body(project)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to create project: ${e.message}"))
        }
    }

    @GetMapping("/team/{teamId}")
    fun getProjectsByTeam(
        @PathVariable teamId: UUID,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false, defaultValue = "title") sortBy: String,
        @RequestParam(required = false, defaultValue = "asc") sortOrder: String
    ): ResponseEntity<Any> {
        return try {
            val projects = projectService.getProjectsByTeam(teamId, page, size, sortBy, sortOrder)
            ResponseEntity.ok(projects)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get projects by team: ${e.message}"))
        }
    }

    @GetMapping("/{projectId}")
    fun getProjectById(@PathVariable projectId: UUID): ResponseEntity<Any> {
        return try {
            val project = projectService.getProjectById(projectId)
            ResponseEntity.ok(project) 
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get project: ${e.message}"))
        }
    }

    @GetMapping("/course/{courseId}")
    fun getProjectsByCourse(
        @PathVariable courseId: UUID,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false, defaultValue = "title") sortBy: String,
        @RequestParam(required = false, defaultValue = "asc") sortOrder: String
    ): ResponseEntity<Any> {
        return try {
            val projects = projectService.getProjectsByCourse(courseId, page, size, sortBy, sortOrder)
            ResponseEntity.ok(projects)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get projects by course: ${e.message}"))
        }
    }

    @PutMapping("/{projectId}")
    fun updateProject(
        @PathVariable projectId: UUID,
        @RequestBody request: UpdateProjectRequest
    ): ResponseEntity<Any> {
        return try {
            val project = projectService.updateProject(projectId, request, request.userId)
            ResponseEntity.ok(project)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to update project: ${e.message}"))
        }
    }

    @PutMapping("/{projectId}/approve")
    fun approve(
        @PathVariable projectId: UUID): ResponseEntity<Any>{
        val userId = SecurityUtils.getCurrentUserId()
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "User not authenticated"))
        }
        return try {
            projectStatusService.approveProject(projectId, userId)
            ResponseEntity.ok(mapOf("success" to true, "message" to "Project approved successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to approve project: ${e.message}"))
        }
    }

    @PutMapping("/{projectId}/reject")
    fun reject(
        @PathVariable projectId: UUID,
    ): ResponseEntity<Any> {
        val userId = SecurityUtils.getCurrentUserId()
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "User not authenticated"))
        }
        return try {
            projectStatusService.rejectProject(projectId, userId)
            ResponseEntity.ok(mapOf("success" to true, "message" to "Project rejected successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to reject project: ${e.message}"))
        }
    }

    @PutMapping("/{projectId}/re-propose")
    fun reProposeProject(@PathVariable projectId: UUID): ResponseEntity<Any> {
        val userId = SecurityUtils.getCurrentUserId()
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "User not authenticated"))
        }
        return try {
            projectStatusService.reProposeProject(projectId, userId)
            ResponseEntity.ok(mapOf("success" to true, "message" to "Project re-proposed successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to re-propose project: ${e.message}"))
        }
    }

    @DeleteMapping("/{projectId}")
    fun deleteProject(
        @PathVariable projectId: UUID,
        @RequestBody request: UserIdRequest
    ): ResponseEntity<Any> {
        return try {
            projectService.deleteProject(projectId, request.userId)
            ResponseEntity.ok(mapOf("success" to true, "message" to "Project deleted successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to delete project: ${e.message}"))
        }
    }

    @GetMapping("/active")
    fun getAllActiveProjects(): ResponseEntity<Any> {
        val rawUserGroup = SecurityUtils.getCurrentJwtClaim("groups")
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to "User not authenticated"))
        val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")

        if(userGroup.equals("admin", ignoreCase = true) || userGroup.equals("manager", ignoreCase = true)) {
            val projects = projectService.getAllActiveProjects()
            return ResponseEntity.ok(projects)
        }
        if(userGroup.equals("faculty", ignoreCase = true)) {
            val currentUserId = SecurityUtils.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf("message" to "User not authenticated"))
            val projects = projectService.getActiveProjectsByFaculty(currentUserId)
            return ResponseEntity.ok(projects)
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("message" to "Unauthorized access - $userGroup role cannot access active projects"))
        }
    }

    @GetMapping("/user/{userId}/course/{courseId}")
    fun getProjectsForUserByCourse(
        @PathVariable userId: String,
        @PathVariable courseId: UUID,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false, defaultValue = "title") sortBy: String,
        @RequestParam(required = false, defaultValue = "asc") sortOrder: String
    ): ResponseEntity<Any> {
        return try {
            val projects = projectService.getProjectsForUserByCourse(userId, courseId, page, size, sortBy, sortOrder)
            ResponseEntity.ok(projects)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get projects for user by course: ${e.message}"))
        }
    }

    @GetMapping("/user/{userId}")
    fun getProjectsForUser(
        @PathVariable userId: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false, defaultValue = "title") sortBy: String,
        @RequestParam(required = false, defaultValue = "asc") sortOrder: String
    ): ResponseEntity<Any> {
        return try {
            val projects = projectService.getProjectsForUser(userId, page, size, sortBy, sortOrder)
            ResponseEntity.ok(projects)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get projects for user: ${e.message}"))
        }
    }

    @GetMapping("/course/{courseId}/search/{userId}")
    fun searchProjectsByCourseForUser(
        @PathVariable courseId: UUID,
        @PathVariable userId: String,
        @RequestParam query: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int
    ): ResponseEntity<Any> {
        return try {
            val projects = projectService.searchProjectsByCourseForUser(userId, courseId, query, page, size)
            ResponseEntity.ok(projects)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to search projects for course: ${e.message}"))
        }
    }

    @GetMapping("/semester/{semesterId}/active")
    fun getActiveProjectsBySemester(@PathVariable semesterId: UUID): ResponseEntity<Any> {
        return try {
            val projects = projectService.getActiveProjectsBySemester(semesterId)
            ResponseEntity.ok(projects)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve active projects for semester: ${e.message}"))
        }
    }

    @GetMapping("/batch/{batchId}/active")
    fun getActiveProjectsByBatch(@PathVariable batchId: UUID): ResponseEntity<Any> {
        return try {
            val projects = projectService.getActiveProjectsByBatch(batchId)
            ResponseEntity.ok(projects)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to retrieve active projects for batch: ${e.message}"))
        }
    }

    @PutMapping("/{projectId}/complete")
    fun completeProject(@PathVariable projectId: UUID): ResponseEntity<Any> {
        val userId = SecurityUtils.getCurrentUserId()
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "User not authenticated"))
        }
        return try {
            projectStatusService.completeProject(projectId, userId)
            ResponseEntity.ok(mapOf("success" to true, "message" to "Project completed successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to complete project: ${e.message}"))
        }
    }

    @PutMapping("/{projectId}/revert-completion")
    fun revertProjectCompletion(@PathVariable projectId: UUID): ResponseEntity<Any> {
        val userId = SecurityUtils.getCurrentUserId()
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf("error" to "User not authenticated"))
        }
        return try {
            projectStatusService.revertProjectCompletion(projectId, userId)
            ResponseEntity.ok(mapOf("success" to true, "message" to "Project completion reverted successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to revert project completion: ${e.message}"))
        }
    }

    @GetMapping("/user/{userId}/archive")
    fun getArchivedProjects(
        @PathVariable userId: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false, defaultValue = "title") sortBy: String,
        @RequestParam(required = false, defaultValue = "asc") sortOrder: String
    ): ResponseEntity<Any> {
        return try {
            val projects = projectStatusService.getArchivedProjects(userId, page, size, sortBy, sortOrder)
            ResponseEntity.ok(projects)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get archived projects: ${e.message}"))
        }
    }

    @GetMapping("/user/{userId}/archive/search")
    fun searchArchivedProjects(
        @PathVariable userId: String,
        @RequestParam query: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false, defaultValue = "title") sortBy: String,
        @RequestParam(required = false, defaultValue = "asc") sortOrder: String
    ): ResponseEntity<Any> {
        return try {
            val projects = projectStatusService.searchArchivedProjects(userId, query, page, size, sortBy, sortOrder)
            ResponseEntity.ok(projects)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to search archived projects: ${e.message}"))
        }
    }

    @GetMapping("/{projectId}/reviews")
    fun getProjectReviews(@PathVariable projectId: UUID): ResponseEntity<Any> {
        return try {
            val userId = SecurityUtils.getCurrentUserId()
            val response = reviewQueryService.checkProjectReviewAssignment(projectId, userId)
            ResponseEntity.ok(response)
        } catch (e: NotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to check project reviews: ${e.message}"))
        }
    }
}
