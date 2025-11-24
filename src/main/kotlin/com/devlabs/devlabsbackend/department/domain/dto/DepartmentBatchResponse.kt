package com.devlabs.devlabsbackend.department.domain.dto

import java.time.Year
import java.util.UUID

data class DepartmentBatchResponse(
    val id: UUID? = null,
    val name: String,
    val joinYear: Year? = null,
    val section: String,
)