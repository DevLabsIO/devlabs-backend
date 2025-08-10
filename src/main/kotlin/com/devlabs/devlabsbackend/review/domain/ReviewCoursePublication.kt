package com.devlabs.devlabsbackend.review.domain

import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.user.domain.User
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "review_course_publication", 
       uniqueConstraints = [UniqueConstraint(columnNames = ["review_id", "course_id"])])
class ReviewCoursePublication(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    var review: Review,

    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "course_id", nullable = false)
    var course: Course,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "published_by_id", nullable = false)
    var publishedBy: User,

    @Column(name = "published_at", nullable = false)
    var publishedAt: LocalDateTime = LocalDateTime.now()
)
