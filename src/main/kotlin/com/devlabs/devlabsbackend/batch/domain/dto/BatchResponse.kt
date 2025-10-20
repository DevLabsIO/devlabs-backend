package com.devlabs.devlabsbackend.batch.domain.dto

import com.devlabs.devlabsbackend.department.domain.dto.DepartmentResponse
import java.time.Year
import java.util.UUID

data class BatchResponse(
    val id: UUID?,
    val name: String,
    val graduationYear: Year,
    val section: String,
    val isActive: Boolean,
    val department: DepartmentResponse?
)