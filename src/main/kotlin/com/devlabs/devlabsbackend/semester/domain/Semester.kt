package com.devlabs.devlabsbackend.semester.domain

import com.devlabs.devlabsbackend.batch.domain.Batch
import com.devlabs.devlabsbackend.course.domain.Course
import com.devlabs.devlabsbackend.user.domain.User
import jakarta.persistence.*
import java.util.*

@Entity
@Table(
    name = "semester",
    indexes = [
        Index(name = "idx_semester_is_active", columnList = "isActive"),
        Index(name = "idx_semester_year", columnList = "year"),
        Index(name = "idx_semester_name_year", columnList = "name, year"),
        Index(name = "idx_semester_created_at", columnList = "createdAt"),
        Index(name = "idx_semester_updated_at", columnList = "updatedAt")
    ]
)
class Semester(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    var name: String,
    var year: Int,
    var isActive: Boolean = true,

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], mappedBy = "semester")
    var courses: MutableList<Course> = mutableListOf(),

    @ManyToMany(cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JoinTable(
        name = "semester_managers",
        joinColumns = [JoinColumn(name = "semester_id")],
        inverseJoinColumns = [JoinColumn(name = "manager_id")]
    )
    var managers: MutableList<User> = mutableListOf(),

    @ManyToMany(mappedBy = "semester", fetch = FetchType.LAZY)
    var batches: MutableSet<Batch> = mutableSetOf(),
    
    val createdAt: java.sql.Timestamp = java.sql.Timestamp.from(java.time.Instant.now()),
    var updatedAt: java.sql.Timestamp = java.sql.Timestamp.from(java.time.Instant.now())
)
