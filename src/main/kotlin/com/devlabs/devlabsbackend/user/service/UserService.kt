package com.devlabs.devlabsbackend.user.service

import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.ConflictException
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.core.pagination.PaginatedResponse
import com.devlabs.devlabsbackend.core.pagination.PaginationInfo
import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import com.devlabs.devlabsbackend.user.domain.dto.*
import com.devlabs.devlabsbackend.user.repository.UserRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

@Service
class UserService(
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun getAllUsersByRole(
        role: Role,
        page: Int,
        size: Int,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<UserResponse> {
        val sort = createSort(sortBy, sortOrder)
        val pageable = PageRequest.of(page, size, sort)
        val userPage = userRepository.findByRolePaged(role.ordinal, pageable)

        return PaginatedResponse(
            data = userPage.content.map { mapToUserResponse(it) },
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = userPage.totalPages,
                total_count = userPage.totalElements.toInt()
            )
        )
    }

    @Transactional(readOnly = true)
    fun getAllUsersByRole(role: Role): List<UserResponse> {
        return userRepository.findByRole(role.ordinal).map { mapToUserResponse(it) }
    }

    @Transactional(readOnly = true)
    @Cacheable(
        value = ["users-list"],
        key = "#page + '_' + #size + '_' + #sortBy + '_' + #sortOrder",
        condition = "#page == 0 && #size == 10 && #sortBy == 'name' && #sortOrder == 'asc'"
    )
    fun getAllUsers(
        page: Int,
        size: Int,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<UserResponse> {
        val sort = createSort(sortBy, sortOrder)
        val pageable = PageRequest.of(page, size, sort)
        val userPage = userRepository.findAllProjected(pageable)

        return PaginatedResponse(
            data = userPage.content.map { mapToUserResponse(it) },
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = userPage.totalPages,
                total_count = userPage.totalElements.toInt()
            )
        )
    }

    @Transactional(readOnly = true)
    fun searchUsers(
        query: String,
        page: Int,
        size: Int,
        sortBy: String = "name",
        sortOrder: String = "asc"
    ): PaginatedResponse<UserResponse> {
        val sort = createSort(sortBy, sortOrder)
        val pageable = PageRequest.of(page, size, sort)
        val userPage = userRepository.searchByNameOrEmailContainingIgnoreCase(query, pageable)

        return PaginatedResponse(
            data = userPage.content.map { mapToUserResponse(it) },
            pagination = PaginationInfo(
                current_page = page + 1,
                per_page = size,
                total_pages = userPage.totalPages,
                total_count = userPage.totalElements.toInt()
            )
        )
    }

    @Cacheable(value = [CacheConfig.USER_DETAIL_CACHE], key = "#userId")
    @Transactional(readOnly = true)
    fun getUserById(userId: String): UserResponse {
        val userData = userRepository.findUserById(userId)
            ?: throw NotFoundException("User with id $userId not found")
        return mapToUserResponse(userData)
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.USER_DETAIL_CACHE], key = "#request.id"),
            CacheEvict(value = ["users-list"], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    @Transactional
    fun createUser(request: CreateUserRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("Email already exists")
        }

        val user = User(
            id = request.id,
            name = request.name,
            email = request.email,
            phoneNumber = request.phoneNumber,
            role = Role.valueOf(request.role.uppercase()),
            isActive = request.isActive
        )

        val savedUser = userRepository.save(user)
        return savedUser.toUserResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.USER_DETAIL_CACHE], key = "#userId"),
            CacheEvict(value = ["users-list"], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAMS_LIST, CacheConfig.TEAM_DETAIL_CACHE], allEntries = true)
        ]
    )
    @Transactional
    fun updateUser(userId: String, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(userId).orElseThrow {
            NotFoundException("User with id $userId not found")
        }

        if (request.email != user.email && userRepository.existsByEmail(request.email)) {
            throw ConflictException("Email already exists")
        }

        user.name = request.name
        user.email = request.email
        user.phoneNumber = request.phoneNumber
        user.role = Role.valueOf(request.role.uppercase())
        user.isActive = request.isActive

        val updatedUser = userRepository.save(user)
        return updatedUser.toUserResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.USER_DETAIL_CACHE], key = "#userId"),
            CacheEvict(value = ["users-list"], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAMS_LIST, CacheConfig.TEAM_DETAIL_CACHE], allEntries = true)
        ]
    )
    @Transactional
    fun deleteUser(userId: String) {
        if (!userRepository.existsById(userId)) {
            throw NotFoundException("User with id $userId not found")
        }
        userRepository.deleteById(userId)
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.USER_DETAIL_CACHE], allEntries = true),
            CacheEvict(value = ["users-list"], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true)
        ]
    )
    @Transactional
    fun bulkDeleteUsers(userIds: List<String>): Map<String, Any> {
        val deletedCount = userIds.count { userRepository.existsById(it) }
        userRepository.deleteAllById(userIds)
        return mapOf(
            "message" to "Users deleted successfully",
            "deletedCount" to deletedCount
        )
    }

    @Transactional(readOnly = true)
    fun checkUserExists(email: String): Boolean {
        return userRepository.existsByEmail(email)
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.USER_DETAIL_CACHE], key = "#request.id"),
            CacheEvict(value = ["users-list"], allEntries = true),
            CacheEvict(value = [CacheConfig.DASHBOARD_ADMIN, CacheConfig.DASHBOARD_MANAGER, CacheConfig.DASHBOARD_STUDENT], allEntries = true),
            CacheEvict(value = [CacheConfig.TEAMS_LIST, CacheConfig.TEAM_DETAIL_CACHE], allEntries = true)
        ]
    )
    @Transactional
    fun createUserFromKeycloakSync(request: KeycloakSyncRequest): UserResponse {
        val existingUser = userRepository.findById(request.id)
        
        if (existingUser.isPresent) {
            val user = existingUser.get()
            user.name = request.name
            user.email = request.email
            user.phoneNumber = request.phoneNumber
            user.role = Role.valueOf(request.role.uppercase())
            user.isActive = request.isActive
            
            val updatedUser = userRepository.save(user)
            return updatedUser.toUserResponse()
        } else {
            val user = User(
                id = request.id,
                name = request.name,
                email = request.email,
                phoneNumber = request.phoneNumber,
                role = Role.valueOf(request.role.uppercase()),
                isActive = request.isActive
            )
            
            val savedUser = userRepository.save(user)
            return savedUser.toUserResponse()
        }
    }

    private fun createSort(sortBy: String, sortOrder: String): Sort {
        val direction = if (sortOrder.lowercase() == "desc") Sort.Direction.DESC else Sort.Direction.ASC
        return Sort.by(direction, sortBy)
    }

    private fun mapToUserResponse(data: Map<String, Any>): UserResponse {
        return UserResponse(
            id = data["id"].toString(),
            name = data["name"].toString(),
            email = data["email"].toString(),
            profileId = data["profile_id"]?.toString(),
            image = data["image"]?.toString(),
            role = data["role"].toString(),
            phoneNumber = data["phone_number"]?.toString(),
            isActive = data["is_active"] as Boolean,
            createdAt = data["created_at"] as Timestamp
        )
    }
}

fun User.toUserResponse(): UserResponse {
    return UserResponse(
        id = this.id!!,
        name = this.name,
        email = this.email,
        profileId = this.profileId,
        image = this.image,
        role = this.role.name,
        phoneNumber = this.phoneNumber,
        isActive = this.isActive,
        createdAt = this.createdAt
    )
}
