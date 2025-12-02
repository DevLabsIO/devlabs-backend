package com.devlabs.devlabsbackend.department.controller

import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.department.domain.dto.*
import com.devlabs.devlabsbackend.department.service.DepartmentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/department")
class DepartmentController(
    private val departmentService: DepartmentService
) {
    
    @GetMapping
    fun getAllDepartments(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "createdAt") sortBy: String,
        @RequestParam(defaultValue = "desc") sortOrder: String
    ): ResponseEntity<Any> {
        return try {
            val departments = departmentService.getAllDepartments(page, size, sortBy, sortOrder)
            ResponseEntity.ok(departments)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to fetch departments: ${e.message}"))
        }
    }
    
    @GetMapping("/search")
    fun searchDepartments(
        @RequestParam query: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortOrder: String?
    ): ResponseEntity<PaginatedResponse<DepartmentResponse>> {
        return try {
            val departments = departmentService.searchDepartments(query, page, size, sortBy, sortOrder)
            ResponseEntity.ok(departments)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(PaginatedResponse(
                    data = emptyList(),
                    pagination = PaginationInfo(0, size, 0, 0)
                ))
        }
    }
    
    @GetMapping("/{departmentId}")
    fun getDepartmentById(@PathVariable departmentId: UUID): ResponseEntity<Any> {
        return try {
            val department = departmentService.getDepartmentById(departmentId)
            ResponseEntity.ok(department)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("message" to "Department not found: ${e.message}"))
        }
    }
    @PostMapping
    fun createDepartment(@RequestBody request: CreateDepartmentRequest): ResponseEntity<Any> {
        return try {
            val department = departmentService.createDepartment(request)
            ResponseEntity.status(HttpStatus.CREATED).body(department)
        } catch (e: IllegalArgumentException) {
            // or your custom ValidationException
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Invalid department data")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to create department: ${e.message}"))
        }
    }

    @PutMapping("/{departmentId}")
    fun updateDepartment(
        @PathVariable departmentId: UUID,
        @RequestBody request: UpdateDepartmentRequest
    ): ResponseEntity<Any> {
        return try {
            val department = departmentService.updateDepartment(departmentId, request)
            ResponseEntity.ok(department)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Invalid department update data")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to update department: ${e.message}"))
        }
    }

    @DeleteMapping("/{departmentId}")
    fun deleteDepartment(@PathVariable departmentId: UUID): ResponseEntity<Any> {
        return try {
            departmentService.deleteDepartment(departmentId)
            ResponseEntity.ok().build()
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Invalid department id")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to delete department: ${e.message}"))
        }
    }

    @GetMapping("/all")
    fun getAllDepartmentsLegacy(): ResponseEntity<Any> {
        return try {
            val departments = departmentService.getAllDepartmentsSimple()
            ResponseEntity.ok(departments)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Invalid request")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to fetch departments: ${e.message}"))
        }
    }

    @GetMapping("/{departmentId}/batches")
    fun getBatches(@PathVariable("departmentId") departmentId: UUID): ResponseEntity<Any> {
        return try {
            val batches = departmentService.getBatchesByDepartmentId(departmentId)
            ResponseEntity.ok(batches)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Invalid department id")))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to fetch batches: ${e.message}"))
        }
    }

}
