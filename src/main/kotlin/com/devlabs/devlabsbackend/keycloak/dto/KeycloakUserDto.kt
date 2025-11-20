package com.devlabs.devlabsbackend.keycloak.dto

data class KeycloakUserDto(
    val id: String,
    val username: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val enabled: Boolean,
    val attributes: Map<String, List<String>>,
    val realmRoles: List<String>,
    val groups: List<String>
)
