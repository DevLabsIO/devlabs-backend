package com.devlabs.devlabsbackend.course.domain.dto

import com.devlabs.devlabsbackend.course.domain.CourseType
import java.time.LocalDate
import java.util.*

data class CourseResponse(
    val id: UUID,
    val name: String,
    val code: String = "",
    val description: String = ""
)

data class CreateCourseRequest(
    val name: String,
    val code: String = "",
    val description: String = "",
    val type: CourseType = CourseType.CORE
)

data class UpdateCourseRequest(
    val name: String? = null,
    val code: String? = null,
    val description: String? = null,
    val type: CourseType? = null
)

data class StudentCourseWithScoresResponse(
    val id: UUID,
    val name: String,
    val code: String = "",
    val description: String = "",
    val averageScorePercentage: Double = 0.0,
    val reviewCount: Int = 0
)

data class CoursePerformanceChartResponse(
    val reviewId: UUID,
    val reviewName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: String,
    val showResult: Boolean,
    val score: Double? = null,
    val totalScore: Double? = null,
    val scorePercentage: Double? = null,
    val courseName: String,
    val courseCode: String
)
