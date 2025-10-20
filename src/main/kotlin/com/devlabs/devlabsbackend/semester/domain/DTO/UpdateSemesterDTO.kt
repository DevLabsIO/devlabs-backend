package com.devlabs.devlabsbackend.semester.domain.dto

data class UpdateSemesterDTO(
    val name: String? = null,
    val year: Int? = null,
    val isActive: Boolean? = null
)
