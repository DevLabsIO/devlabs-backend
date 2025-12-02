package com.devlabs.devlabsbackend.batch.service

import com.devlabs.devlabsbackend.batch.domain.Batch
import com.devlabs.devlabsbackend.batch.domain.dto.BatchResponse
import com.devlabs.devlabsbackend.batch.domain.dto.CreateBatchRequest
import com.devlabs.devlabsbackend.batch.domain.dto.UpdateBatchRequest
import com.devlabs.devlabsbackend.batch.repository.BatchRepository
import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.department.domain.dto.DepartmentResponse
import com.devlabs.devlabsbackend.department.repository.DepartmentRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.dto.UserResponse
import com.devlabs.devlabsbackend.user.repository.UserRepository
import com.devlabs.devlabsbackend.user.service.toUserResponse
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Year
import java.util.*

@Service
class BatchService(
    private val userRepository: UserRepository,
    private val batchRepository: BatchRepository,
    private val departmentRepository: DepartmentRepository,
) {
    
    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.BATCH_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCHES_LIST_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCH_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCHES_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DEPARTMENTS_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER], allEntries = true)
        ]
    )
    fun createBatch(request: CreateBatchRequest): BatchResponse {
        val department = request.departmentId?.let { departmentId ->
            departmentRepository.findById(departmentId).orElseThrow {
                NotFoundException("Department with id $departmentId not found")
            }
        }
        val batchName = if (department != null) {
            "${request.joinYear.value}${department.name}${request.section}"
        } else {
            request.name
        }

        val batch = Batch(
            name = batchName,
            joinYear = request.joinYear,
            section = request.section,
            isActive = request.isActive,
            department = department,
        )

        val savedBatch = batchRepository.save(batch)
        return savedBatch.toBatchResponse()
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.BATCH_DETAIL_CACHE], key = "#batchId")
    fun getBatchById(batchId: UUID): BatchResponse {
        val batch = batchRepository.findById(batchId).orElseThrow {
            NotFoundException("Batch with id $batchId not found")
        }
        return batch.toBatchResponse()
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.BATCH_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCHES_LIST_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCHES_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DEPARTMENTS_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSE_BATCHES_CACHE], allEntries = true)
        ]
    )
    fun updateBatch(batchId: UUID, request: UpdateBatchRequest): BatchResponse {
        val batch = batchRepository.findById(batchId).orElseThrow {
            NotFoundException("Batch with id $batchId not found")
        }

        request.joinYear?.let { batch.joinYear = it }
        request.section?.let { batch.section = it }
        request.isActive?.let { batch.isActive = it }
        request.departmentId?.let { departmentId ->
            val department = departmentRepository.findById(departmentId).orElseThrow {
                NotFoundException("Department with id $departmentId not found")
            }
            batch.department = department
        }

        if (request.joinYear != null || request.section != null || request.departmentId != null) {
            val department = batch.department
            if (department != null) {
                batch.name = "${batch.joinYear.value}${department.name}${batch.section}"
            }
        } else {
            request.name?.let { batch.name = it }
        }

        val savedBatch = batchRepository.save(batch)
        return savedBatch.toBatchResponse()
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.BATCH_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCHES_LIST_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCH_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCHES_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DEPARTMENTS_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN], allEntries = true)
        ]
    )
    fun deleteBatch(batchId: UUID) {
        val batch = batchRepository.findById(batchId).orElseThrow {
            NotFoundException("Batch with id $batchId not found")
        }
        batchRepository.delete(batch)
    }

    @Cacheable(
        value = [CacheConfig.BATCHES_LIST_CACHE],
        key = "'batches-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortOrder + '-' + #isActive",
        condition = "#page == 0 && #size == 10 && #isActive == null"
    )
    fun getAllBatches(
        isActive: Boolean?,
        page: Int = 0,
        size: Int = 10,
        sortBy: String? = "createdAt",
        sortOrder: String? = "desc",
    ): PaginatedResponse<BatchResponse> {
        val actualSortBy = sortBy ?: "createdAt"
        val actualSortOrder = sortOrder?.uppercase() ?: "ASC"
        val offset = page * size
        
        val batchIds = if (isActive != null) {
            batchRepository.findBatchIdsByIsActive(isActive, actualSortBy, actualSortOrder, offset, size)
        } else {
            batchRepository.findBatchIdsOnly(actualSortBy, actualSortOrder, offset, size)
        }
        val totalCount = if (isActive != null) {
            batchRepository.countBatchesByIsActive(isActive)
        } else {
            batchRepository.countAllBatches()
        }

        if (batchIds.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0,
                )
            )
        }

        val batchesData = batchRepository.findBatchListData(batchIds)
        val batchResponses = batchesData.map { mapToBatchResponse(it) }

        return PaginatedResponse(
            data = batchResponses,
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt(),
            )
        )
    }

    fun searchBatches(
        query: String,
        isActive: Boolean?,
        page: Int = 0,
        size: Int = 10,
        sortBy: String? = null,
        sortOrder: String? = null,
    ): PaginatedResponse<BatchResponse> {
        val actualSortBy = sortBy ?: "createdAt"
        val actualSortOrder = sortOrder?.uppercase() ?: "ASC"
        val offset = page * size
        
        val batchIds = if (isActive != null) {
            batchRepository.searchBatchIdsByIsActive(query, isActive, actualSortBy, actualSortOrder, offset, size)
        } else {
            batchRepository.searchBatchIdsOnly(query, actualSortBy, actualSortOrder, offset, size)
        }
        val totalCount = if (isActive != null) {
            batchRepository.countSearchBatchesByIsActive(query, isActive)
        } else {
            batchRepository.countSearchBatches(query)
        }

        if (batchIds.isEmpty()) {
            return PaginatedResponse(
                data = emptyList(),
                pagination = PaginationInfo(
                    current_page = page,
                    per_page = size,
                    total_pages = 0,
                    total_count = 0,
                )
            )
        }

        val batchesData = batchRepository.findBatchListData(batchIds)
        val batchResponses = batchesData.map { mapToBatchResponse(it) }

        return PaginatedResponse(
            data = batchResponses,
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt(),
            )
        )
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.BATCHES_CACHE], key = "'active_all_' + (#instructorId ?: 'global')")
    fun getAllActiveBatches(instructorId: String? = null): List<BatchResponse> {
        if (instructorId != null) {
            val ids = batchRepository.findActiveBatchIdsByInstructor(instructorId)
            if (ids.isEmpty()) {
                return emptyList()
            }
            val batchesData = batchRepository.findBatchListData(ids)
            val batchesByIdMap = batchesData.associateBy { UUID.fromString(it["id"].toString()) }
            return ids.mapNotNull { id ->
                batchesByIdMap[id]?.let { mapToBatchResponse(it) }
            }
        } else {
            val batchesData = batchRepository.findActiveBatchesNative()
            return batchesData.map { mapToBatchResponse(it) }
        }
    }
    

    @Cacheable(
        value = [CacheConfig.BATCH_STUDENTS_CACHE],
        key = "'batch-' + #batchId + '-students-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortOrder"
    )
    @Transactional(readOnly = true)
    fun getBatchStudents(
        batchId: UUID,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "name",
        sortOrder: String = "asc",
    ): PaginatedResponse<UserResponse> {
        batchRepository.findById(batchId).orElseThrow {
            NotFoundException("Batch with id $batchId not found")
        }

        val offset = page * size
        val sortByNormalized = if (sortBy == "createdAt") "createdAt" else sortBy
        val sortOrderNormalized = sortOrder.uppercase()

        val students = batchRepository.findStudentsByBatchIdNative(batchId, sortByNormalized, sortOrderNormalized, offset, size)
        val totalCount = batchRepository.countStudentsByBatchId(batchId)

        return PaginatedResponse(
            data = students.map { mapToUserResponse(it) },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt(),
            )
        )
    }

    @Transactional(readOnly = true)
    fun searchBatchStudents(
        batchId: UUID,
        query: String,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "name",
        sortOrder: String = "asc",
    ): PaginatedResponse<UserResponse> {
        batchRepository.findById(batchId).orElseThrow {
            NotFoundException("Batch with id $batchId not found")
        }

        val offset = page * size
        val sortByNormalized = if (sortBy == "createdAt") "createdAt" else sortBy
        val sortOrderNormalized = sortOrder.uppercase()

        val students = batchRepository.searchStudentsByBatchIdNative(batchId, query, sortByNormalized, sortOrderNormalized, offset, size)
        val totalCount = batchRepository.countSearchStudentsByBatchId(batchId, query)

        return PaginatedResponse(
            data = students.map { mapToUserResponse(it) },
            pagination = PaginationInfo(
                current_page = page,
                per_page = size,
                total_pages = ((totalCount + size - 1) / size).toInt(),
                total_count = totalCount.toInt(),
            )
        )
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.BATCH_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCHES_LIST_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCH_STUDENTS_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSE_STUDENTS_CACHE, CacheConfig.COURSES_USER_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun addStudentsToBatch(batchId: UUID, studentId: List<String>) {
        val batch = batchRepository.findById(batchId).orElseThrow {
            NotFoundException("Could not find batch with id $batchId")
        }
        val users = userRepository.findAllById(studentId)
        batch.students.addAll(users)
        batchRepository.save(batch)
    }

    @Transactional
    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.BATCH_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCHES_LIST_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.BATCH_STUDENTS_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.COURSE_STUDENTS_CACHE, CacheConfig.COURSES_USER_CACHE], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    fun removeStudentsFromBatch(batchId: UUID, studentId: List<String>) {
        val batch = batchRepository.findById(batchId).orElseThrow {
            NotFoundException("Could not find batch with id $batchId")
        }
        val users = userRepository.findAllById(studentId)
        batch.students.removeAll(users)
        batchRepository.save(batch)
    }

    @Transactional(readOnly = true)
    fun getAvailableStudents(batchId: UUID, query: String): List<UserResponse> {
        batchRepository.findById(batchId).orElseThrow {
            NotFoundException("Batch with id $batchId not found")
        }

        return batchRepository.findAvailableStudentsForBatch(batchId, query)
            .map { mapToUserResponse(it) }
    }

    private fun mapToBatchResponse(data: Map<String, Any>): BatchResponse {
        val departmentId = data["department_id"]?.let { UUID.fromString(it.toString()) }
        return BatchResponse(
            id = UUID.fromString(data["id"].toString()),
            name = data["name"].toString(),
            joinYear = Year.of((data["join_year"] as Number).toInt()),
            section = data["section"].toString(),
            isActive = data["is_active"] as Boolean,
            department = departmentId?.let {
                DepartmentResponse(
                    id = it,
                    name = data["department_name"]?.toString() ?: ""
                )
            }
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
}

fun Batch.toBatchResponse(): BatchResponse {
    return BatchResponse(
        id = this.id,
        name = this.name,
        joinYear = this.joinYear,
        section = this.section,
        isActive = this.isActive,
        department = this.department?.let {
            DepartmentResponse(
                id = it.id!!,
                name = it.name,
            )
        },
    )
}
