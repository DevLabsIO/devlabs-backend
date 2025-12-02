package com.devlabs.devlabsbackend.project.domain

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.team.domain.Team
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.sql.Timestamp
import java.time.Instant
import java.util.*

enum class ProjectStatus {
    PROPOSED,
    ONGOING,
    COMPLETED,
    REJECTED
}

data class ProjectReference(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val url: String? = null,
    val description: String? = null
)

@Entity
@Table(
    name = "project",
    indexes = [
        Index(name = "idx_project_status", columnList = "status"),
        Index(name = "idx_project_team_id", columnList = "team_id"),
        Index(name = "idx_project_created_at", columnList = "createdAt"),
        Index(name = "idx_project_updated_at", columnList = "updatedAt"),
        Index(name = "idx_project_status_updated", columnList = "status, updatedAt")
    ]
)
class Project(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    var title: String,
    
    @Column(columnDefinition = "TEXT")
    var description: String,
    
    @Column(columnDefinition = "TEXT")
    var objectives: String? = null,
    
    var githubUrl: String? = null,
    
    var status: ProjectStatus = ProjectStatus.PROPOSED,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    var team: Team,
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "project_courses",
        joinColumns = [JoinColumn(name = "project_id")],
        inverseJoinColumns = [JoinColumn(name = "course_id")]
    )
    var courses: MutableSet<Course> = mutableSetOf(),
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "review_project",
        joinColumns = [JoinColumn(name = "project_id")],
        inverseJoinColumns = [JoinColumn(name = "review_id")]
    )
    var reviews: MutableSet<Review> = mutableSetOf(),
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "project_references", columnDefinition = "jsonb")
    var references: MutableList<ProjectReference>? = mutableListOf(),
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "uploaded_files", columnDefinition = "jsonb")
    var uploadedFiles: MutableList<String>? = mutableListOf(),
    
    val createdAt: Timestamp = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp = Timestamp.from(Instant.now())
)
