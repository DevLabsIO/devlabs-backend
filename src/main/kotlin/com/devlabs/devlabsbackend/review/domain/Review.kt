package com.devlabs.devlabsbackend.review.domain

import com.devlabs.devlabsbackend.batch.domain.Batch
import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.rubrics.domain.Rubrics
import com.devlabs.devlabsbackend.user.domain.User
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "review")
class Review (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,
    var name: String,
    var startDate: LocalDate,
    var endDate: LocalDate,
    
    @Column(name = "is_published")
    var isPublished: Boolean = false,
    
    @Column(name = "published_at")
    var publishedAt: LocalDateTime? = null,
    
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

    @ManyToOne
    @JoinColumn(name = "rubrics_id")
    var rubrics: Rubrics? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "review_project",
        joinColumns = [JoinColumn(name = "review_id")],
        inverseJoinColumns = [JoinColumn(name = "project_id")]
    )
    val projects: MutableSet<Project> = mutableSetOf(),

    var files: MutableSet<String> = mutableSetOf()
)