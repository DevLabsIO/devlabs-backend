package com.devlabs.devlabsbackend.rubrics.domain

import com.devlabs.devlabsbackend.criterion.domain.Criterion
import com.devlabs.devlabsbackend.user.domain.User
import jakarta.persistence.*
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "rubrics",
    indexes = [
        Index(name = "idx_rubrics_created_by_id", columnList = "created_by_id"),
        Index(name = "idx_rubrics_is_shared", columnList = "isShared"),
        Index(name = "idx_rubrics_created_at", columnList = "createdAt")
    ]
)
class Rubrics(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    var name: String,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    var createdBy: User,
    
    var createdAt: Timestamp = Timestamp.from(Instant.now()),

    @OneToMany(mappedBy = "rubrics", cascade = [CascadeType.ALL], orphanRemoval = true)
    var criteria: MutableSet<Criterion> = mutableSetOf(),
    
    var isShared: Boolean = false
)
