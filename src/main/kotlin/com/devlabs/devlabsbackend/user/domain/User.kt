package com.devlabs.devlabsbackend.user.domain

import jakarta.persistence.*
import java.sql.Timestamp
import java.time.Instant

enum class Role {
    STUDENT,
    ADMIN,
    FACULTY,
    MANAGER
}

@Entity
@Table(
    name = "\"user\"",
    indexes = [
        Index(name = "idx_user_email", columnList = "email"),
        Index(name = "idx_user_profile_id", columnList = "profileId"),
        Index(name = "idx_user_role", columnList = "role"),
        Index(name = "idx_user_is_active", columnList = "isActive"),
        Index(name = "idx_user_created_at", columnList = "createdAt")
    ]
)
class User (
    @Id
    val id: String? = null,
    var name: String,
    var email: String,
    var profileId: String? = null,
    var image: String? = null,    
    var role: Role,
    var phoneNumber: String?,
    var isActive: Boolean = true,
    var createdAt: Timestamp = Timestamp.from(Instant.now()),
)
