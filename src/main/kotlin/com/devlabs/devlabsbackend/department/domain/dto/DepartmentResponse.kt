package com.devlabs.devlabsbackend.department.domain.dto

import java.util.*

data class DepartmentResponse(
    val id: UUID,
    val name: String,
    val batches: List<DepartmentBatchResponse> = emptyList()
)