package com.devlabs.devlabsbackend.dashboard.controller

import com.devlabs.devlabsbackend.dashboard.service.AdminDashboardResponse
import com.devlabs.devlabsbackend.dashboard.service.DashboardService
import com.devlabs.devlabsbackend.dashboard.service.ManagerStaffDashboardResponse
import com.devlabs.devlabsbackend.dashboard.service.StudentDashboardResponse
import com.devlabs.devlabsbackend.security.utils.SecurityUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val dashboardService: DashboardService
) {

    @GetMapping("/admin")
    fun getAdminDashboard(): ResponseEntity<AdminDashboardResponse> {
        return try {
            val rawUserGroup = SecurityUtils.getCurrentJwtClaim("groups")
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
            
            val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")
            
            if (!userGroup.equals("admin", ignoreCase = true)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null)
            }
            
            val dashboard = dashboardService.getAdminDashboard()
            ResponseEntity.ok(dashboard)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }

    @GetMapping("/manager-staff")
    fun getManagerStaffDashboard(): ResponseEntity<ManagerStaffDashboardResponse> {
        return try {
            val rawUserGroup = SecurityUtils.getCurrentJwtClaim("groups")
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
            
            val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")
            
            if (!userGroup.equals("manager", ignoreCase = true) && !userGroup.equals("faculty", ignoreCase = true)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null)
            }
            
            val currentUserId = SecurityUtils.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
            
            val dashboard = dashboardService.getManagerStaffDashboard(currentUserId)
            ResponseEntity.ok(dashboard)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }

    @GetMapping("/student")
    fun getStudentDashboard(): ResponseEntity<StudentDashboardResponse> {
        return try {
            val rawUserGroup = SecurityUtils.getCurrentJwtClaim("groups")
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
            
            val userGroup = rawUserGroup.trim().removePrefix("[/").removeSuffix("]")
            
            if (!userGroup.equals("student", ignoreCase = true)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null)
            }
            
            val currentUserId = SecurityUtils.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null)
            
            val dashboard = dashboardService.getStudentDashboard(currentUserId)
            ResponseEntity.ok(dashboard)
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }
}