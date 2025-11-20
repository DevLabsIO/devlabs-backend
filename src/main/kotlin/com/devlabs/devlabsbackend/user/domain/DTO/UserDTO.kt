package com.devlabs.devlabsbackend.user.domain.dto

import com.fasterxml.jackson.annotation.JsonTypeName
import java.io.Serializable
import java.sql.Timestamp

@JsonTypeName("UserResponse")
data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val profileId: String?,
    val image: String?,
    val role: String,
    val phoneNumber: String?,
    val isActive: Boolean,
    val createdAt: Timestamp
) : Serializable

data class CreateUserRequest(
    val id: String,
    val name: String,
    val email: String,
    val phoneNumber: String?,
    val role: String,
    val isActive: Boolean = true
)

data class UpdateUserRequest(
    val name: String,
    val email: String,
    val phoneNumber: String?,
    val role: String,
    val isActive: Boolean
)
