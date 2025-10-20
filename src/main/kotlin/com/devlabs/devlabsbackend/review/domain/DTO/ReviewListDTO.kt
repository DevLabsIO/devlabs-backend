package com.devlabs.devlabsbackend.review.domain.dto

import com.devlabs.devlabsbackend.user.domain.Role
import java.time.LocalDate
import java.util.*

data class ReviewListDTO(
    val id: UUID,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdById: String?,
    val createdByName: String?,
    val createdByEmail: String?,
    val createdByRole: Role?,
    val rubricsId: UUID?,
    val rubricsName: String?,
    val courseCount: Long,
    val projectCount: Long,
    val publishedCourseCount: Long
)
