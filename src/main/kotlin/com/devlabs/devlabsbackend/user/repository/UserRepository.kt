package com.devlabs.devlabsbackend.user.repository

import com.devlabs.devlabsbackend.user.domain.Role
import com.devlabs.devlabsbackend.user.domain.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, String> {

    @Query(value = """
        SELECT u.id, u.name, u.email, u.profile_id, u.image,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM "user" u
        WHERE u.id = :id
    """, nativeQuery = true)
    fun findUserById(@Param("id") id: String): Map<String, Any>?

    @Query(value = """
        SELECT u.id, u.name, u.email, u.profile_id, u.image,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM "user" u
        WHERE u.role = :role
        ORDER BY 
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN u.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN u.created_at END DESC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN u.name END ASC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN u.name END DESC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'ASC' THEN u.email END ASC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'DESC' THEN u.email END DESC NULLS LAST
    """, nativeQuery = true)
    fun findByRolePaged(@Param("role") role: Int, @Param("sortBy") sortBy: String, @Param("sortOrder") sortOrder: String, pageable: Pageable): Page<Map<String, Any>>

    @Query(value = """
        SELECT u.id, u.name, u.email, u.profile_id, u.image,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM "user" u
        WHERE u.role IN :roles
        AND (:isActive IS NULL OR u.is_active = :isActive)
        ORDER BY 
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN u.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN u.created_at END DESC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN u.name END ASC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN u.name END DESC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'ASC' THEN u.email END ASC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'DESC' THEN u.email END DESC NULLS LAST
    """, nativeQuery = true)
    fun findByRolesAndIsActivePaged(
        @Param("roles") roles: List<Int>,
        @Param("isActive") isActive: Boolean?,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        pageable: Pageable
    ): Page<Map<String, Any>>

    @Query(value = """
        SELECT u.id, u.name, u.email, u.profile_id, u.image,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM "user" u
        WHERE (:isActive IS NULL OR u.is_active = :isActive)
        ORDER BY 
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN u.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN u.created_at END DESC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN u.name END ASC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN u.name END DESC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'ASC' THEN u.email END ASC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'DESC' THEN u.email END DESC NULLS LAST
    """, nativeQuery = true)
    fun findAllProjectedWithIsActive(
        @Param("isActive") isActive: Boolean?,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        pageable: Pageable
    ): Page<Map<String, Any>>

    @Query(value = """
        SELECT u.id, u.name, u.email, u.profile_id, u.image,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM "user" u
        WHERE u.role = :role
    """, nativeQuery = true)
    fun findByRole(@Param("role") role: Int): List<Map<String, Any>>

    @Query(value = """
        SELECT u.id, u.name, u.email, u.profile_id, u.image,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM "user" u
        ORDER BY 
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN u.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN u.created_at END DESC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN u.name END ASC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN u.name END DESC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'ASC' THEN u.email END ASC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'DESC' THEN u.email END DESC NULLS LAST
    """, nativeQuery = true)
    fun findAllProjected(@Param("sortBy") sortBy: String, @Param("sortOrder") sortOrder: String, pageable: Pageable): Page<Map<String, Any>>

    @Query(value = """
        SELECT u.id, u.name, u.email, u.profile_id, u.image,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM "user" u
        WHERE (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
        AND (:isActive IS NULL OR u.is_active = :isActive)
        ORDER BY 
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN u.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN u.created_at END DESC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN u.name END ASC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN u.name END DESC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'ASC' THEN u.email END ASC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'DESC' THEN u.email END DESC NULLS LAST
    """, nativeQuery = true)
    fun searchByNameOrEmailContainingIgnoreCase(
        @Param("query") query: String,
        @Param("isActive") isActive: Boolean?,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        pageable: Pageable
    ): Page<Map<String, Any>>

    @Query(value = """
        SELECT u.id, u.name, u.email, u.profile_id, u.image,
               CASE u.role
                   WHEN 0 THEN 'STUDENT'
                   WHEN 1 THEN 'ADMIN'
                   WHEN 2 THEN 'FACULTY'
                   WHEN 3 THEN 'MANAGER'
               END as role,
               u.phone_number, u.is_active, u.created_at
        FROM "user" u
        WHERE (LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
        AND u.role IN :roles
        AND (:isActive IS NULL OR u.is_active = :isActive)
        ORDER BY 
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'ASC' THEN u.created_at END ASC NULLS LAST,
            CASE WHEN :sortBy = 'createdAt' AND :sortOrder = 'DESC' THEN u.created_at END DESC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'ASC' THEN u.name END ASC NULLS LAST,
            CASE WHEN :sortBy = 'name' AND :sortOrder = 'DESC' THEN u.name END DESC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'ASC' THEN u.email END ASC NULLS LAST,
            CASE WHEN :sortBy = 'email' AND :sortOrder = 'DESC' THEN u.email END DESC NULLS LAST
    """, nativeQuery = true)
    fun searchByNameOrEmailAndRolesAndIsActive(
        @Param("query") query: String,
        @Param("roles") roles: List<Int>,
        @Param("isActive") isActive: Boolean?,
        @Param("sortBy") sortBy: String,
        @Param("sortOrder") sortOrder: String,
        pageable: Pageable
    ): Page<Map<String, Any>>

    fun existsByEmail(email: String): Boolean

    fun findByEmail(email: String): User?
    fun findByProfileId(profileId: String): User?

    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    fun findByNameOrEmailContainingIgnoreCase(@Param("query") query: String): List<User>

    override fun findAll(pageable: Pageable): Page<User>

    fun countByRole(role: Role): Long

    fun findTop5ByOrderByCreatedAtDesc(): List<User>
}
