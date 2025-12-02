package com.devlabs.devlabsbackend.core.constants
import com.devlabs.devlabsbackend.user.domain.Role

object RoleConstants {
    const val STUDENT = 0   // Role.STUDENT.ordinal
    const val ADMIN = 1     // Role.ADMIN.ordinal
    const val FACULTY = 2   // Role.FACULTY.ordinal
    const val MANAGER = 3   // Role.MANAGER.ordinal
    
    fun getOrdinal(role: Role): Int = role.ordinal
    
    fun fromOrdinal(ordinal: Int): Role = Role.values()[ordinal]
}
