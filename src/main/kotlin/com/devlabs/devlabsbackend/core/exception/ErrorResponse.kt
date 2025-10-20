package com.devlabs.devlabsbackend.core.exception

import java.time.LocalDateTime

data class ErrorResponse(
    val message: String,
    val statusCode: Int,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class ValidationErrorResponse(
    val message: String,
    val statusCode: Int,
    val errors: Map<String, String>,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
