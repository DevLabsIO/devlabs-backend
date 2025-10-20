package com.devlabs.devlabsbackend.criterion.domain

import com.devlabs.devlabsbackend.rubrics.domain.Rubrics
import jakarta.persistence.*
import java.util.*

@Entity
@Table(
    name = "criterion",
    indexes = [
        Index(name = "idx_criterion_rubrics_id", columnList = "rubrics_id"),
        Index(name = "idx_criterion_is_common", columnList = "isCommon")
    ]
)
class Criterion(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    var name: String,
    
    var description: String,
    
    var maxScore: Float,
    
    var isCommon: Boolean,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubrics_id")
    var rubrics: Rubrics? = null
)
