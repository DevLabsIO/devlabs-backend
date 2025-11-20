package com.devlabs.devlabsbackend.user.domain.dto

data class SyncStatsResponse(
    val keycloakUserCount: Int,
    val dbUserCount: Int,
    val unsyncedUserCount: Int,
    val unsyncedUsers: List<UnsyncedUserDto>
)

data class UnsyncedUserDto(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val username: String,
    val enabled: Boolean,
    val roles: List<String>,
    val groups: List<String>,
    val profileId: String?,
    val phoneNumber: String?
)

data class SyncRequest(
    val userIds: List<String>?
)

data class SyncResponse(
    val success: Boolean,
    val syncedCount: Int,
    val message: String
)
