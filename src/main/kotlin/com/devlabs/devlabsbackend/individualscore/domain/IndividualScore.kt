package com.devlabs.devlabsbackend.individualscore.domain

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.criterion.domain.Criterion
import com.devlabs.devlabsbackend.project.domain.Project
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.user.domain.User
import jakarta.persistence.*
import java.util.*

@Entity
@Table(
    name = "individual_score",
    indexes = [
        Index(name = "idx_individual_score_participant", columnList = "participant_id"),
        Index(name = "idx_individual_score_review", columnList = "review_id"),
        Index(name = "idx_individual_score_project", columnList = "project_id"),
        Index(name = "idx_individual_score_course", columnList = "course_id"),
        Index(name = "idx_individual_score_criterion", columnList = "criterion_id"),
        Index(name = "idx_individual_score_participant_review", columnList = "participant_id, review_id"),
        Index(name = "idx_individual_score_participant_course", columnList = "participant_id, course_id")
    ]
)
class IndividualScore(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id")
    val participant: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id")
    val criterion: Criterion,

    @Column(name = "score")
    var score: Double,

    @Column(columnDefinition = "TEXT")
    var comment: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    val review: Review,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    val project: Project,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = true)
    val course: Course? = null
)
