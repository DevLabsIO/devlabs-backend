package com.devlabs.devlabsbackend.user.controller

import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.security.utils.SecurityUtils
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.dto.*
import com.devlabs.devlabsbackend.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(private val userService: UserService) {

    @GetMapping
    fun getAllUsers(
        @RequestParam(required = false) role: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) sort_by: String?,
        @RequestParam(required = false) sort_order: String?
    ): ResponseEntity<Any> {
        return try {
            val actualSortBy = if (sort_by.isNullOrBlank()) "name" else sort_by
            val actualSortOrder = if (sort_order.isNullOrBlank()) "asc" else sort_order
            
            if (role != null) {
                try {
                    val roleEnum = Role.valueOf(role.uppercase())
                    val paginatedUsers = userService.getAllUsersByRole(roleEnum, page, size, actualSortBy, actualSortOrder)
                    ResponseEntity.ok(paginatedUsers)
                } catch (e: IllegalArgumentException) {
                    ResponseEntity.badRequest().body(
                        mapOf("message" to "Invalid role: $role. Valid roles are: ${Role.values().joinToString(", ")}")
                    )
                }
            } else {
                val paginatedUsers = userService.getAllUsers(page, size, actualSortBy, actualSortOrder)
                ResponseEntity.ok(paginatedUsers)
            }
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to fetch users: ${e.message}"))
        }
    }

    @GetMapping("/search")
    fun searchUsers(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) sort_by: String?,
        @RequestParam(required = false) sort_order: String?
    ): ResponseEntity<Any> {
        return try {
            val actualSortBy = if (sort_by.isNullOrBlank()) "name" else sort_by
            val actualSortOrder = if (sort_order.isNullOrBlank()) "asc" else sort_order
            val paginatedUsers = userService.searchUsers(query, page, size, actualSortBy, actualSortOrder)
            ResponseEntity.ok(paginatedUsers)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to search users: ${e.message}"))
        }
    }

    @PostMapping
    fun createUser(@RequestBody request: CreateUserRequest): ResponseEntity<Any> {
        return try {
            val user = userService.createUser(request)
            ResponseEntity.status(HttpStatus.CREATED).body(user)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to create user: ${e.message}"))
        }
    }

    @PutMapping("/{userId}")
    fun updateUser(
        @PathVariable userId: String,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<Any> {
        return try {
            val user = userService.updateUser(userId, request)
            ResponseEntity.ok(user)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to update user: ${e.message}"))
        }
    }

    @DeleteMapping("/{userId}")
    fun deleteUser(@PathVariable userId: String): ResponseEntity<Any> {
        return try {
            userService.deleteUser(userId)
            ResponseEntity.ok(mapOf("message" to "User deleted successfully"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to delete user: ${e.message}"))
        }
    }

    @DeleteMapping("/bulk")
    fun deleteUsers(@RequestBody users: List<String>): ResponseEntity<Any> {
        return ResponseEntity(
            userService.bulkDeleteUsers(users),
            HttpStatus.OK
        )
    }

    @GetMapping("/{userId}")
    fun getUserById(@PathVariable userId: String): ResponseEntity<Any> {
        return try {
            val user = userService.getUserById(userId)
            ResponseEntity.ok(user)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to fetch user: ${e.message}"))
        }
    }

    @GetMapping("/faculty")
    fun getAllFaculty(): ResponseEntity<Any> {
        return try {
            val faculty = userService.getAllUsersByRole(Role.FACULTY)
            ResponseEntity.ok(faculty)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to fetch faculty: ${e.message}"))
        }
    }

    @GetMapping("/check-exists")
    fun checkUserExists(@RequestParam email: String): ResponseEntity<Map<String, Boolean>> {
        return try {
            val exists = userService.checkUserExists(email)
            ResponseEntity.ok(mapOf("exists" to exists))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("exists" to false))
        }
    }

    @PostMapping("/keycloak-sync")
    fun createUserFromKeycloakSync(@RequestBody request: KeycloakUserSyncRequest): ResponseEntity<Any> {
        return try {
            val userIdFromToken = SecurityUtils.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(mapOf("error" to "User not authenticated"))

            val keycloakSyncRequest = KeycloakSyncRequest(
                id = userIdFromToken,
                name = request.name,
                email = request.email,
                role = request.role,
                phoneNumber = request.phoneNumber,
                isActive = request.isActive
            )

            val user = userService.createUserFromKeycloakSync(keycloakSyncRequest)
            ResponseEntity.status(HttpStatus.CREATED).body(user)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to e.message))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to create user from Keycloak sync: ${e.message}"))
        }
    }

    @GetMapping("/sync-stats")
    fun getSyncStats(): ResponseEntity<Any> {
        return try {
            val stats = userService.getSyncStats()
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to fetch sync stats: ${e.message}"))
        }
    }

    @PostMapping("/sync-from-keycloak")
    fun syncFromKeycloak(@RequestBody request: SyncRequest): ResponseEntity<Any> {
        return try {
            val response = userService.syncUsersFromKeycloak(request.userIds)
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to "Failed to sync users: ${e.message}"))
        }
    }
}
