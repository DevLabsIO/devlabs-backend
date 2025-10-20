package com.devlabs.devlabsbackend.criterion.repository

import com.devlabs.devlabsbackend.criterion.domain.Criterion
import com.devlabs.devlabsbackend.rubrics.domain.Rubrics
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CriterionRepository : JpaRepository<Criterion, UUID> {
    fun findAllByRubrics(rubrics: Rubrics): List<Criterion>
}
