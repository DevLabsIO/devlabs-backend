package com.devlabs.devlabsbackend.department.domain

import com.devlabs.devlabsbackend.batch.domain.Batch
import jakarta.persistence.*
import java.util.*

@Entity
@Table(
    name = "department",
    indexes = [
        Index(name = "idx_department_name", columnList = "name")
    ]
)
class Department (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    var name: String,
    @OneToMany(mappedBy = "department", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val batches: MutableSet<Batch> = mutableSetOf(),
)
