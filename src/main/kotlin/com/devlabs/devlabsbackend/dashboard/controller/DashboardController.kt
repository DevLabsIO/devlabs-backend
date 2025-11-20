package com.devlabs.devlabsbackend.dashboard.controller

import com.devlabs.devlabsbackend.dashboard.domain.dto.AdminDashboardResponse
import com.devlabs.devlabsbackend.dashboard.domain.dto.ManagerStaffDashboardResponse
import com.devlabs.devlabsbackend.dashboard.domain.dto.StudentDashboardResponse
import com.devlabs.devlabsbackend.dashboard.service.DashboardService
import com.devlabs.devlabsbackend.security.utils.SecurityUtils
import org.springframework.cache.CacheManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val dashboardService: DashboardService,
    private val cacheManager: CacheManager
) {

    @GetMapping("/admin")
    fun getAdminDashboard(): ResponseEntity<AdminDashboardResponse> {
        val rawUserGroup = SecurityUtils.getCurrentJwtClaim("groups")
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
        
        val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")
        
        if (!userGroup.equals("admin", ignoreCase = true)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null)
        }
        
        val dashboard = dashboardService.getAdminDashboard()
        return ResponseEntity.ok(dashboard)
    }

    @GetMapping("/manager-staff")
    fun getManagerStaffDashboard(): ResponseEntity<ManagerStaffDashboardResponse> {
        val rawUserGroup = SecurityUtils.getCurrentJwtClaim("groups")
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
        
        val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")
        
        if (!userGroup.equals("manager", ignoreCase = true) && !userGroup.equals("faculty", ignoreCase = true)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null)
        }
        
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
        
        val dashboard = dashboardService.getManagerStaffDashboard(currentUserId)
        return ResponseEntity.ok(dashboard)
    }

    @GetMapping("/student")
    fun getStudentDashboard(): ResponseEntity<StudentDashboardResponse> {
        val rawUserGroup = SecurityUtils.getCurrentJwtClaim("groups")
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
        
        val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")
        
        if (!userGroup.equals("student", ignoreCase = true)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null)
        }
        
        val currentUserId = SecurityUtils.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
        
        val dashboard = dashboardService.getStudentDashboard(currentUserId)
        return ResponseEntity.ok(dashboard)
    }

    @DeleteMapping("/cache/clear")
    fun clearDashboardCaches(): ResponseEntity<Map<String, Any>> {
        val rawUserGroup = SecurityUtils.getCurrentJwtClaim("groups")
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
        
        val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")
        
        if (!userGroup.equals("admin", ignoreCase = true)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null)
        }
        
        val clearedCaches = mutableListOf<String>()
        
        listOf(
            "dashboard-admin",
            "dashboard-manager", 
            "dashboard-student"
        ).forEach { cacheName ->
            cacheManager.getCache(cacheName)?.clear()
            clearedCaches.add(cacheName)
        }
        
        return ResponseEntity.ok(mapOf(
            "message" to "✅ Dashboard caches cleared successfully!",
            "clearedCaches" to clearedCaches,
            "timestamp" to System.currentTimeMillis()
        ))
    }
}
