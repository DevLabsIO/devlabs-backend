package com.devlabs.devlabsbackend.department.service

import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.department.domain.Department
import com.devlabs.devlabsbackend.department.domain.dto.*
import com.devlabs.devlabsbackend.department.repository.DepartmentRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class DepartmentService(
    private val departmentRepository: DepartmentRepository
) {
    
    @Transactional(readOnly = true)
    @Cacheable(
        value = ["departments-list"],
        key = "'departments-' + #page + '-' + #size + '-' + #sort_by + '-' + #sort_order",
        condition = "#page == 0 && #size == 10"
    )
    fun getAllDepartments(
        page: Int = 0, 
        size: Int = 10, 
        sort_by: String = "createdAt", 
        sort_order: String = "desc"
    ): PaginatedResponse<DepartmentResponse> {
        val offset = page * size
        val sortOrderNormalized = sort_order.uppercase()
        
        val departmentIds = departmentRepository.findAllIds(sort_by, sortOrderNormalized, offset, size)
        val totalCount = departmentRepository.count()
        
        if (departmentIds.isEmpty()) {
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
        
        val departments = departmentRepository.findAllByIdWithBatches(departmentIds)
        val deptMap = departments.associateBy { it.id }
        val orderedDepts = departmentIds.mapNotNull { deptMap[it] }
        
        return PaginatedResponse(
            data = orderedDepts.map { it.toDepartmentResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }
    
    @Transactional(readOnly = true)
    fun searchDepartments(
        query: String,
        page: Int = 0,
        size: Int = 10,
        sort_by: String? = null,
        sort_order: String? = null
    ): PaginatedResponse<DepartmentResponse> {
        val offset = page * size
        val sortBy = sort_by ?: "createdAt"
        val sortOrderNormalized = (sort_order ?: "desc").uppercase()
        
        val departmentIds = departmentRepository.findIdsByNameContaining(query, sortBy, sortOrderNormalized, offset, size)
        val totalCount = departmentRepository.countByNameContaining(query)
        
        if (departmentIds.isEmpty()) {
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
        
        val departments = departmentRepository.findAllByIdWithBatches(departmentIds)
        val deptMap = departments.associateBy { it.id }
        val orderedDepts = departmentIds.mapNotNull { deptMap[it] }
        
        return PaginatedResponse(
            data = orderedDepts.map { it.toDepartmentResponse() },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt()
            )
        )
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.DEPARTMENT_DETAIL_CACHE], key = "#departmentId")
    fun getDepartmentById(departmentId: UUID): DepartmentResponse {
        val department = departmentRepository.findByIdWithBatches(departmentId)
            ?: throw NotFoundException("Department with id $departmentId not found")
        
        return department.toDepartmentResponse()
    }
    
    @Caching(evict = [
        CacheEvict(value = [CacheConfig.DEPARTMENT_DETAIL_CACHE, CacheConfig.DEPARTMENTS_LIST_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DEPARTMENTS_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN], allEntries = true)
    ])
    fun createDepartment(request: CreateDepartmentRequest): DepartmentResponse {
        val department = Department(
            name = request.name
        )
        
        val savedDepartment = departmentRepository.save(department)
        return savedDepartment.toDepartmentResponse()
    }
    
    @Caching(evict = [
        CacheEvict(value = [CacheConfig.DEPARTMENT_DETAIL_CACHE, CacheConfig.DEPARTMENTS_LIST_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DEPARTMENTS_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN], allEntries = true)
    ])
    fun updateDepartment(departmentId: UUID, request: UpdateDepartmentRequest): DepartmentResponse {
        val department = departmentRepository.findById(departmentId)
            .orElseThrow { NotFoundException("Department with id $departmentId not found") }
        
        request.name?.let { department.name = it }
        
        val savedDepartment = departmentRepository.save(department)
        return savedDepartment.toDepartmentResponse()
    }
    
    @Caching(evict = [
        CacheEvict(value = ["department-detail", "departments-list"], allEntries = true),
        CacheEvict(value = [CacheConfig.DEPARTMENT_DETAIL_CACHE, CacheConfig.DEPARTMENTS_LIST_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DEPARTMENTS_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.BATCH_DETAIL_CACHE, CacheConfig.BATCHES_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN], allEntries = true)
    ])
    fun deleteDepartment(departmentId: UUID) {
        val department = departmentRepository.findById(departmentId)
            .orElseThrow { NotFoundException("Department with id $departmentId not found") }
        
        departmentRepository.delete(department)
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.DEPARTMENTS_CACHE], key = "'all_simple'")
    fun getAllDepartmentsSimple(): List<SimpleDepartmentResponse> {
        return departmentRepository.findAll().map { department ->
            SimpleDepartmentResponse(
                id = department.id!!,
                name = department.name
            )
        }
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.DEPARTMENTS_CACHE], key = "'dept_batches_' + #departmentId")
    fun getBatchesByDepartmentId(departmentId: UUID): List<DepartmentBatchResponse> {
        return departmentRepository.findBatchesByDepartmentId(departmentId)
    }
}

fun Department.toDepartmentResponse(): DepartmentResponse {
    val batchResponses = this.batches.map { batch ->
        DepartmentBatchResponse(
            id = batch.id,
            name = batch.name,
            joinYear = batch.joinYear,
            section = batch.section
        )
    }
    
    return DepartmentResponse(
        id = this.id,
        name = this.name,
        batches = batchResponses
    )
}
