package com.devlabs.devlabsbackend.team.domain

import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.user.domain.User
import jakarta.persistence.*
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "team",
    indexes = [
        Index(name = "idx_team_name", columnList = "name"),
        Index(name = "idx_team_created_at", columnList = "createdAt"),
        Index(name = "idx_team_updated_at", columnList = "updatedAt")
    ]
)
class Team(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    @Column(nullable = false)
    var name: String,
    
    var description: String? = null,
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "team_members",
        joinColumns = [JoinColumn(name = "team_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")]
    )
    var members: MutableSet<User> = mutableSetOf(),
    
    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    var projects: MutableSet<Project> = mutableSetOf(),
    
    val createdAt: Timestamp = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp = Timestamp.from(Instant.now())
)
