package com.devlabs.devlabsbackend.course.domain

import com.devlabs.devlabsbackend.batch.domain.Batch
import com.devlabs.devlabsbackend.review.domain.Review
import com.devlabs.devlabsbackend.semester.domain.Semester
import com.devlabs.devlabsbackend.user.domain.User
import jakarta.persistence.*
import java.util.*

enum class CourseType {
    CORE,
    ELECTIVE,
    MICRO_CREDENTIAL
}

@Entity
@Table(
    name = "course",
    indexes = [
        Index(name = "idx_course_semester_id", columnList = "semester_id"),
        Index(name = "idx_course_name", columnList = "name"),
        Index(name = "idx_course_code", columnList = "code"),
        Index(name = "idx_course_type", columnList = "type")
    ]
)
class Course(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    var name: String,
    var code: String = "",
    var description: String,
    var type: CourseType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    var semester: Semester,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "course_student",
        joinColumns = [JoinColumn(name = "course_id")],
        inverseJoinColumns = [JoinColumn(name = "student_id")]
    )
    var students: MutableSet<User> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "course_instructor",
        joinColumns = [JoinColumn(name = "course_id")],
        inverseJoinColumns = [JoinColumn(name = "instructor_id")]
    )
    var instructors: MutableSet<User> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "course_batch",
        joinColumns = [JoinColumn(name = "course_id")],
        inverseJoinColumns = [JoinColumn(name = "batch_id")]
    )
    var batches: MutableSet<Batch> = mutableSetOf(),

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "courses")
    var reviews: MutableSet<Review> = mutableSetOf(),
)
