package com.devlabs.devlabsbackend.batch.domain.dto

import java.time.Year
import java.util.*

data class CreateBatchRequest(
    val name: String,
    val joinYear: Year,
    val section: String,
    val isActive: Boolean,
    val departmentId: UUID? = null
)

data class UpdateBatchRequest(
    val name: String? = null,
    val joinYear: Year? = null,
    val section: String? = null,
    val isActive: Boolean? = null,
    val departmentId: UUID? = null
)