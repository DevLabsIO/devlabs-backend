package com.devlabs.devlabsbackend.keycloak.service

import com.devlabs.devlabsbackend.keycloak.config.KeycloakAdminConfig
import com.devlabs.devlabsbackend.keycloak.dto.KeycloakUserDto
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.resource.RealmResource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class KeycloakAdminService(private val config: KeycloakAdminConfig) {

    private val logger = LoggerFactory.getLogger(KeycloakAdminService::class.java)
    private var lastAuthTime: Long = 0
    private val authTimeout = 58_000L

    private val keycloak by lazy {
        val builder = KeycloakBuilder.builder()
            .serverUrl(config.serverUrl)

        if (!config.username.isNullOrBlank() && !config.password.isNullOrBlank()) {
            builder
                .realm("master")
                .username(config.username)
                .password(config.password)
                .clientId("admin-cli")
        } else if (!config.clientId.isNullOrBlank() && !config.clientSecret.isNullOrBlank()) {
            builder
                .realm(config.realm)
                .clientId(config.clientId)
                .clientSecret(config.clientSecret)
                .grantType("client_credentials")
        } else {
            throw IllegalArgumentException("Either username/password or clientId/clientSecret must be provided for Keycloak admin authentication")
        }

        builder.build()
    }

    private fun getRealm(): RealmResource {
        val now = Instant.now().toEpochMilli()
        if (now - lastAuthTime > authTimeout) {
            keycloak.tokenManager().grantToken()
            lastAuthTime = now
        }
        return keycloak.realm(config.realm)
    }

    fun getAllUsers(): List<KeycloakUserDto> {
        return try {
            val realm = getRealm()
            val pageSize = 1000
            var firstResult = 0
            val allUsers = mutableListOf<org.keycloak.representations.idm.UserRepresentation>()

            // Fetch all users using pagination
            do {
                val usersPage = realm.users().list(firstResult, pageSize)
                if (usersPage.isEmpty()) break
                allUsers.addAll(usersPage)
                firstResult += pageSize
            } while (usersPage.size == pageSize)

            logger.info("Fetched ${allUsers.size} total users from Keycloak")

            // Map users without fetching roles/groups (defer to getUserById for detailed info)
            allUsers.mapNotNull { user ->
                try {
                    val userId = user.id ?: return@mapNotNull null

                    KeycloakUserDto(
                        id = userId,
                        username = user.username ?: "",
                        email = user.email,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        enabled = user.isEnabled ?: false,
                        attributes = user.attributes ?: emptyMap(),
                        realmRoles = emptyList(),
                        groups = emptyList()
                    )
                } catch (e: Exception) {
                    logger.warn("Failed to map user representation: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to fetch users from Keycloak: ${e.message}", e)
            throw RuntimeException("Failed to fetch users from Keycloak")
        }
    }

    fun getUserById(userId: String): KeycloakUserDto? {
        return try {
            val realm = getRealm()
            val user = realm.users().get(userId).toRepresentation()

            val realmRoles = realm.users().get(userId)
                .roles()
                .realmLevel()
                .listAll()
                .mapNotNull { it.name }

            val groups = realm.users().get(userId)
                .groups()
                .mapNotNull { it.path ?: it.name }

            KeycloakUserDto(
                id = user.id,
                username = user.username ?: "",
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                enabled = user.isEnabled ?: false,
                attributes = user.attributes ?: emptyMap(),
                realmRoles = realmRoles,
                groups = groups
            )
        } catch (e: Exception) {
            logger.error("Failed to fetch user from Keycloak: ${e.message}", e)
            null
        }
    }
}