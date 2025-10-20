package com.devlabs.devlabsbackend.security.utils

import com.devlabs.devlabsbackend.user.domain.Role
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt

object SecurityUtils {

    fun getCurrentUserId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication

        return if (authentication != null && authentication.isAuthenticated) {
            if (authentication.principal is Jwt) {
                (authentication.principal as Jwt).subject
            } else {
                authentication.name
            }
        } else {
            null
        }
    }

    fun getCurrentJwt(): Jwt? {
        val authentication = SecurityContextHolder.getContext().authentication

        return if (authentication != null && authentication.isAuthenticated) {
            authentication.principal as? Jwt
        } else {
            null
        }
    }

    fun getCurrentJwtClaim(claimName: String): String? {
        val jwt = getCurrentJwt()
        return jwt?.getClaimAsString(claimName)
    }
    
    fun getCurrentUserRoleFromJwt(): Role? {
        val jwt = getCurrentJwt() ?: return null
        
        val groups = jwt.getClaimAsStringList("groups") ?: return null
        
        return when {
            groups.any { it.contains("admin", ignoreCase = true) } -> Role.ADMIN
            groups.any { it.contains("manager", ignoreCase = true) } -> Role.MANAGER
            groups.any { it.contains("faculty", ignoreCase = true) } -> Role.FACULTY
            groups.any { it.contains("student", ignoreCase = true) } -> Role.STUDENT
            else -> null
        }
    }

    fun getCurrentUserNameFromJwt(): String? {
        return getCurrentJwtClaim("name")
    }
    
    fun getCurrentUserEmailFromJwt(): String? {
        return getCurrentJwtClaim("email")
    }
}
