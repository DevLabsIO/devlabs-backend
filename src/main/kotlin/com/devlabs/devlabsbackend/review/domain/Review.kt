package com.devlabs.devlabsbackend.review.domain

import com.devlabs.devlabsbackend.batch.domain.Batch
import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.rubrics.domain.Rubrics
import com.devlabs.devlabsbackend.user.domain.User
import jakarta.persistence.*
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.util.*

@Entity
@Table(
    name = "review",
    indexes = [
        Index(name = "idx_review_start_date", columnList = "startDate"),
        Index(name = "idx_review_end_date", columnList = "endDate"),
        Index(name = "idx_review_created_by_id", columnList = "created_by_id"),
        Index(name = "idx_review_rubrics_id", columnList = "rubrics_id"),
        Index(name = "idx_review_dates", columnList = "startDate, endDate"),
        Index(name = "idx_review_created_at", columnList = "createdAt"),
        Index(name = "idx_review_updated_at", columnList = "updatedAt")
    ]
)
class Review(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    
    var name: String,
    
    var startDate: LocalDate,
    
    var endDate: LocalDate,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "course_review",
        joinColumns = [JoinColumn(name = "review_id")],
        inverseJoinColumns = [JoinColumn(name = "course_id")]
    )
    val courses: MutableSet<Course> = mutableSetOf(),
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "review_batch",
        joinColumns = [JoinColumn(name = "review_id")],
        inverseJoinColumns = [JoinColumn(name = "batch_id")]
    )
    val batches: MutableSet<Batch> = mutableSetOf(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rubrics_id")
    var rubrics: Rubrics? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "review_project",
        joinColumns = [JoinColumn(name = "review_id")],
        inverseJoinColumns = [JoinColumn(name = "project_id")]
    )
    val projects: MutableSet<Project> = mutableSetOf(),

    @ElementCollection
    @CollectionTable(name = "review_files", joinColumns = [JoinColumn(name = "review_id")])
    @Column(name = "file_path")
    var files: MutableSet<String> = mutableSetOf(),
    
    val createdAt: Timestamp = Timestamp.from(Instant.now()),
    var updatedAt: Timestamp = Timestamp.from(Instant.now())
)
