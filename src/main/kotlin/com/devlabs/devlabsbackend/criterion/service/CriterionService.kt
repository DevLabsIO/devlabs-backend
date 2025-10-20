package com.devlabs.devlabsbackend.criterion.service

import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.ForbiddenException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.criterion.domain.Criterion
import com.devlabs.devlabsbackend.criterion.domain.dto.CriterionResponse
import com.devlabs.devlabsbackend.criterion.repository.CriterionRepository
import com.devlabs.devlabsbackend.rubrics.repository.RubricsRepository
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class CriterionService(
    private val criterionRepository: CriterionRepository,
    private val rubricsRepository: RubricsRepository,
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.RUBRICS_CACHE], key = "'criterion:' + #id")
    fun getCriterionById(id: UUID): CriterionResponse {
        val criterion = criterionRepository.findById(id).orElseThrow {
            NotFoundException("Criterion with id $id not found")
        }
        
        return criterion.toCriterionResponse()
    }

    @Transactional(readOnly = true)
    @Cacheable(value = [CacheConfig.RUBRICS_CACHE], key = "'rubrics-criteria:' + #rubricsId")
    fun getCriteriaByRubricsId(rubricsId: UUID): List<CriterionResponse> {
        val rubrics = rubricsRepository.findById(rubricsId).orElseThrow {
            NotFoundException("Rubrics with id $rubricsId not found")
        }
        
        val criteria = criterionRepository.findAllByRubrics(rubrics)
        return criteria.map { it.toCriterionResponse() }
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = [CacheConfig.RUBRICS_CACHE], key = "'criterion:' + #id"),
        CacheEvict(value = [CacheConfig.RUBRICS_LIST_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.RUBRICS_DETAIL_CACHE], allEntries = true),
        CacheEvict(value = [CacheConfig.RUBRICS_CACHE], allEntries = true)
    ])
    fun deleteCriterion(id: UUID, userId: String) {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        if (user.role != Role.ADMIN && user.role != Role.MANAGER) {
            throw ForbiddenException("Only admin or manager can delete criterion")
        }
        
        val criterion = criterionRepository.findById(id).orElseThrow {
            NotFoundException("Criterion with id $id not found")
        }

        criterion.rubrics?.let { rubrics ->
            rubrics.criteria.remove(criterion)
        }

        criterion.rubrics = null
        criterionRepository.save(criterion)
        criterionRepository.delete(criterion)
    }
}

fun Criterion.toCriterionResponse(): CriterionResponse {
    return CriterionResponse(
        id = this.id,
        name = this.name,
        description = this.description,
        maxScore = this.maxScore,
        isCommon = this.isCommon
    )
}
