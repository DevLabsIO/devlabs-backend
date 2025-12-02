package com.devlabs.devlabsbackend.department.domain

import com.devlabs.devlabsbackend.batch.domain.Batch
import jakarta.persistence.*
import java.util.*

@Entity
@Table(
    name = "department",
    indexes = [
        Index(name = "idx_department_name", columnList = "name"),
        Index(name = "idx_department_created_at", columnList = "createdAt"),
        Index(name = "idx_department_updated_at", columnList = "updatedAt")
    ]
)
class Department (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    var name: String,
    @OneToMany(mappedBy = "department", cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.LAZY, orphanRemoval = false)
    val batches: MutableSet<Batch> = mutableSetOf(),
    val createdAt: java.sql.Timestamp = java.sql.Timestamp.from(java.time.Instant.now()),
    var updatedAt: java.sql.Timestamp = java.sql.Timestamp.from(java.time.Instant.now())
)
