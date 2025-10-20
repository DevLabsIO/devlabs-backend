package com.devlabs.devlabsbackend.rubrics.service

import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.ConflictException
import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.criterion.domain.Criterion
import com.devlabs.devlabsbackend.criterion.domain.dto.CriterionResponse
import com.devlabs.devlabsbackend.criterion.repository.CriterionRepository
import com.devlabs.devlabsbackend.rubrics.domain.Rubrics
import com.devlabs.devlabsbackend.rubrics.domain.dto.*
import com.devlabs.devlabsbackend.rubrics.repository.RubricsRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class RubricsService(
    private val rubricsRepository: RubricsRepository,
    private val criterionRepository: CriterionRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    @Cacheable(
        value = [CacheConfig.RUBRICS_LIST_CACHE],
        key = "#page + '-' + #size + '-' + #sortBy + '-' + #sortDirection",
        condition = "#page == 0 && #size == 10"
    )
    fun getAllRubrics(
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortDirection: String = "desc"
    ): PaginatedResponse<RubricsResponse> {
        val sort = if (sortDirection.equals("desc", ignoreCase = true)) {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }
        
        val pageable = PageRequest.of(page, size, sort)
        val rubricsPage = rubricsRepository.findAllProjected(pageable)
        
        val rubricsResponses = rubricsPage.content.map { projection ->
            projection.toRubricsResponse()
        }
        
        return PaginatedResponse(
            data = rubricsResponses,
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = rubricsPage.totalPages,
                total_count = rubricsPage.totalElements.toInt()
            )
        )
    }
    
    @Transactional(readOnly = true)
    fun searchRubrics(
        searchTerm: String,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortDirection: String = "desc"
    ): PaginatedResponse<RubricsResponse> {
        val sort = if (sortDirection.equals("desc", ignoreCase = true)) {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }
        
        val pageable = PageRequest.of(page, size, sort)
        val rubricsPage = rubricsRepository.searchProjected(searchTerm, pageable)
        
        val rubricsResponses = rubricsPage.content.map { projection ->
            projection.toRubricsResponse()
        }
        
        return PaginatedResponse(
            data = rubricsResponses,
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = rubricsPage.totalPages,
                total_count = rubricsPage.totalElements.toInt()
            )
        )
    }
    
    @Transactional(readOnly = true)
    @Cacheable(
        value = ["rubrics-list"],
        key = "'user-' + #userId + '-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortDirection",
        condition = "#page == 0 && #size == 10"
    )
    fun getRubricsByUser(
        userId: String,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortDirection: String = "desc"
    ): PaginatedResponse<RubricsResponse> {
        userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }
        
        val sort = if (sortDirection.equals("desc", ignoreCase = true)) {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }
        
        val pageable = PageRequest.of(page, size, sort)
        val rubricsPage = rubricsRepository.findByCreatedByProjected(userId, pageable)
        
        val rubricsResponses = rubricsPage.content.map { projection ->
            projection.toRubricsResponse()
        }
        
        return PaginatedResponse(
            data = rubricsResponses,
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = rubricsPage.totalPages,
                total_count = rubricsPage.totalElements.toInt()
            )
        )
    }
    
    @Transactional(readOnly = true)
    @Cacheable(
        value = ["rubrics-list"],
        key = "'shared-' + #page + '-' + #size + '-' + #sortBy + '-' + #sortDirection",
        condition = "#page == 0 && #size == 10"
    )
    fun getSharedRubrics(
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortDirection: String = "desc"
    ): PaginatedResponse<RubricsResponse> {
        val sort = if (sortDirection.equals("desc", ignoreCase = true)) {
            Sort.by(sortBy).descending()
        } else {
            Sort.by(sortBy).ascending()
        }
        
        val pageable = PageRequest.of(page, size, sort)
        val rubricsPage = rubricsRepository.findBySharedProjected(pageable)
        
        val rubricsResponses = rubricsPage.content.map { projection ->
            projection.toRubricsResponse()
        }
        
        return PaginatedResponse(
            data = rubricsResponses,
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = rubricsPage.totalPages,
                total_count = rubricsPage.totalElements.toInt()
            )
        )
    }

    
    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.RUBRICS_DETAIL_CACHE], key = "#rubricsId")
    fun getRubricsById(rubricsId: UUID): RubricsDetailResponse {
        val rubrics = rubricsRepository.findByIdWithDetails(rubricsId)
            ?: throw NotFoundException("Rubrics with id $rubricsId not found")
        
        return mapToDetailResponse(rubrics)
    }

    @Transactional
    @CacheEvict(value = [CacheConfig.RUBRICS_LIST_CACHE, CacheConfig.RUBRICS_DETAIL_CACHE], allEntries = true)
    fun createRubrics(request: CreateRubricsRequest): RubricsDetailResponse {
        val user = userRepository.findById(request.userId).orElseThrow {
            NotFoundException("User with id ${request.userId} not found")
        }
        
        if (user.role != Role.ADMIN && user.role != Role.MANAGER && user.role != Role.FACULTY) {
            throw ForbiddenException("Only admin, manager, or faculty can create rubrics")
        }
        
        if (request.isShared && user.role != Role.ADMIN && user.role != Role.MANAGER) {
            throw ForbiddenException("Only admin or manager can create shared rubrics")
        }
        
        val rubrics = Rubrics(
            name = request.name,
            createdBy = user,
            isShared = request.isShared
        )
        
        val savedRubrics = rubricsRepository.save(rubrics)
        
        request.criteria.forEach { criterionRequest ->
            val criterion = Criterion(
                name = criterionRequest.name,
                description = criterionRequest.description,
                maxScore = criterionRequest.maxScore,
                isCommon = criterionRequest.isCommon,
                rubrics = savedRubrics
            )
            
            val savedCriterion = criterionRepository.save(criterion)
            savedRubrics.criteria.add(savedCriterion)
        }
        
        return mapToDetailResponse(savedRubrics)
    }
    
    @Transactional
    @CacheEvict(value = [CacheConfig.RUBRICS_LIST_CACHE, CacheConfig.RUBRICS_DETAIL_CACHE], allEntries = true)
    fun updateRubrics(rubricsId: UUID, request: UpdateRubricsRequest): RubricsDetailResponse {
        val user = userRepository.findById(request.userId).orElseThrow {
            NotFoundException("User with id ${request.userId} not found")
        }
        
        val rubrics = rubricsRepository.findByIdWithDetails(rubricsId)
            ?: throw NotFoundException("Rubrics with id $rubricsId not found")
        
        when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
            }
            Role.FACULTY -> {
                if (rubrics.createdBy.id != user.id) {
                    throw ForbiddenException("Faculty can only edit rubrics they created")
                }
                if (request.isShared != rubrics.isShared) {
                    throw ForbiddenException("Faculty cannot change shared status of rubrics")
                }
            }
            else -> throw ForbiddenException("You don't have permission to update rubrics")
        }
        
        rubrics.name = request.name
        
        if (user.role == Role.ADMIN || user.role == Role.MANAGER) {
            rubrics.isShared = request.isShared
        }
        
        val existingCriteria = criterionRepository.findAllByRubrics(rubrics)
        existingCriteria.forEach {
            it.rubrics = null
            criterionRepository.delete(it)
        }
        
        rubrics.criteria.clear()
        
        request.criteria.forEach { criterionRequest ->
            val criterion = Criterion(
                name = criterionRequest.name,
                description = criterionRequest.description,
                maxScore = criterionRequest.maxScore,
                isCommon = criterionRequest.isCommon,
                rubrics = rubrics
            )
            
            val savedCriterion = criterionRepository.save(criterion)
            rubrics.criteria.add(savedCriterion)
        }
        
        val updatedRubrics = rubricsRepository.save(rubrics)
        return mapToDetailResponse(updatedRubrics)
    }
    
    @Transactional
    @CacheEvict(value = [CacheConfig.RUBRICS_LIST_CACHE, CacheConfig.RUBRICS_DETAIL_CACHE], allEntries = true)
    fun deleteRubrics(rubricsId: UUID, userId: String) {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }
        
        val rubrics = rubricsRepository.findByIdWithDetails(rubricsId)
            ?: throw NotFoundException("Rubrics with id $rubricsId not found")
        
        when (user.role) {
            Role.ADMIN, Role.MANAGER -> {
            }
            Role.FACULTY -> {
                if (rubrics.createdBy.id != user.id) {
                    throw ForbiddenException("Faculty can only delete rubrics they created")
                }
            }
            else -> throw ForbiddenException("You don't have permission to delete rubrics")
        }
        
        val criteria = criterionRepository.findAllByRubrics(rubrics)
        criteria.forEach {
            it.rubrics = null
            criterionRepository.delete(it)
        }
        rubricsRepository.delete(rubrics)
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.RUBRICS_LIST_CACHE], key = "'user-combined-' + #userId")
    fun getRubricsByUserCombined(userId: String): List<RubricsDetailResponse> {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException("User with id $userId not found")
        }
        
        val userRubrics = rubricsRepository.findByCreatedByWithDetails(userId)
        val sharedRubrics = rubricsRepository.findByIsSharedTrueWithDetails()
        
        val combinedRubrics = (userRubrics + sharedRubrics.filter { shared ->
            userRubrics.none { it.id == shared.id }
        }).distinctBy { it.id }
        
        return combinedRubrics.map { mapToDetailResponse(it) }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.RUBRICS_LIST_CACHE], key = "'all-rubrics'")
    fun getAllRubricsNonPaginated(): List<RubricsDetailResponse> {
        return rubricsRepository.findAllWithCriteria().map { mapToDetailResponse(it) }
    }

    private fun mapToDetailResponse(rubrics: Rubrics): RubricsDetailResponse {
        return RubricsDetailResponse(
            id = rubrics.id,
            name = rubrics.name,
            createdBy = SimpleCreatedByInfo(
                id = rubrics.createdBy.id!!,
                name = rubrics.createdBy.name,
                role = rubrics.createdBy.role.name
            ),
            createdAt = rubrics.createdAt,
            isShared = rubrics.isShared,
            criteria = rubrics.criteria.map { criterion ->
                CriterionResponse(
                    id = criterion.id,
                    name = criterion.name,
                    description = criterion.description,
                    maxScore = criterion.maxScore,
                    isCommon = criterion.isCommon
                )
            }
        )
    }
}

fun RubricsListProjection.toRubricsResponse(): RubricsResponse {
    return RubricsResponse(
        id = this.getId(),
        name = this.getName(),
        createdBy = SimpleCreatedByInfo(
            id = this.getCreatedById(),
            name = this.getCreatedByName(),
            role = this.getCreatedByRole()
        ),
        createdAt = this.getCreatedAt(),
        isShared = this.getIsShared()
    )
}
