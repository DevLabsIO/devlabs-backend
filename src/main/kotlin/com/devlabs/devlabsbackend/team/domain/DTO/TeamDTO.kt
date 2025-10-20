package com.devlabs.devlabsbackend.team.domain.dto

import com.devlabs.devlabsbackend.user.domain.dto.UserResponse
import java.sql.Timestamp
import java.util.*

data class TeamResponse(
    val id: UUID?,
    val name: String,
    val description: String?,
    val members: List<UserResponse>,
    val projectCount: Int,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)

data class CreateTeamRequest(
    val name: String,
    val description: String?,
    val creatorId: String,
    val memberIds: List<String> = emptyList()
)

data class UpdateTeamRequest(
    val name: String?,
    val description: String?,
    val memberIds: List<String>?
)
