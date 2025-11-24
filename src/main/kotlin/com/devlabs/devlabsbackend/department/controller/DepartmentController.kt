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
        @RequestParam(defaultValue = "createdAt") sort_by: String,
        @RequestParam(defaultValue = "desc") sort_order: String
    ): ResponseEntity<Any> {
        return try {
            val departments = departmentService.getAllDepartments(page, size, sort_by, sort_order)
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
        @RequestParam(required = false) sort_by: String?,
        @RequestParam(required = false) sort_order: String?
    ): ResponseEntity<PaginatedResponse<DepartmentResponse>> {
        return try {
            val departments = departmentService.searchDepartments(query, page, size, sort_by, sort_order)
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
    fun createDepartment(@RequestBody request: CreateDepartmentRequest): DepartmentResponse {
        return departmentService.createDepartment(request)
    }

    @PutMapping("/{departmentId}")
    fun updateDepartment(
        @PathVariable departmentId: UUID,
        @RequestBody request: UpdateDepartmentRequest
    ): DepartmentResponse {
        return departmentService.updateDepartment(departmentId, request)
    }

    @DeleteMapping("/{departmentId}")
    fun deleteDepartment(@PathVariable departmentId: UUID): ResponseEntity<Any> {
        departmentService.deleteDepartment(departmentId)
        return ResponseEntity.ok().build()
    }
    
    @GetMapping("/all")
    fun getAllDepartmentsLegacy(): List<SimpleDepartmentResponse> {
        return departmentService.getAllDepartmentsSimple()
    }

    @GetMapping("/{departmentId}/batches")
    fun getBatches(@PathVariable("departmentId") departmentId: UUID): List<DepartmentBatchResponse> {
        return departmentService.getBatchesByDepartmentId(departmentId)
    }
}
