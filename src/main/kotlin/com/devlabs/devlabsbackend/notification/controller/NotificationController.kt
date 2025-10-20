package com.devlabs.devlabsbackend.notification.controller

import com.devlabs.devlabsbackend.notification.domain.dto.CreateNotificationRequest
import com.devlabs.devlabsbackend.notification.domain.dto.NotificationResponse
import com.devlabs.devlabsbackend.notification.domain.dto.UpdateNotificationRequest
import com.devlabs.devlabsbackend.notification.service.NotificationService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun getAllNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<NotificationResponse>> {
        val pageable = PageRequest.of(page, size)
        val notifications = notificationService.getAllNotifications(pageable)
        return ResponseEntity.ok(notifications)
    }

    @GetMapping("/{id}")
    fun getNotificationById(@PathVariable id: UUID): ResponseEntity<NotificationResponse> {
        val notification = notificationService.getNotificationById(id)
        return ResponseEntity.ok(notification)
    }

    @GetMapping("/user/{userId}")
    fun getNotificationsByUser(
        @PathVariable userId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<NotificationResponse>> {
        val pageable = PageRequest.of(page, size)
        val notifications = notificationService.getNotificationsByUser(userId, pageable)
        return ResponseEntity.ok(notifications)
    }

    @GetMapping("/user/{userId}/unread")
    fun getUnreadNotifications(
        @PathVariable userId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<Page<NotificationResponse>> {
        val pageable = PageRequest.of(page, size)
        val notifications = notificationService.getUnreadNotifications(userId, pageable)
        return ResponseEntity.ok(notifications)
    }

    @GetMapping("/user/{userId}/unread/count")
    fun getUnreadCount(@PathVariable userId: UUID): ResponseEntity<Map<String, Long>> {
        val count = notificationService.getUnreadCount(userId)
        return ResponseEntity.ok(mapOf("count" to count))
    }

    @PostMapping
    fun createNotification(@RequestBody request: CreateNotificationRequest): ResponseEntity<NotificationResponse> {
        val notification = notificationService.createNotification(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(notification)
    }

    @PutMapping("/{id}")
    fun updateNotification(
        @PathVariable id: UUID,
        @RequestBody request: UpdateNotificationRequest
    ): ResponseEntity<NotificationResponse> {
        val notification = notificationService.updateNotification(id, request)
        return ResponseEntity.ok(notification)
    }

    @DeleteMapping("/{id}")
    fun deleteNotification(@PathVariable id: UUID): ResponseEntity<Map<String, String>> {
        notificationService.deleteNotification(id)
        return ResponseEntity.ok(mapOf("message" to "Notification deleted successfully"))
    }

    @PutMapping("/user/{userId}/mark-all-read")
    fun markAllAsRead(@PathVariable userId: UUID): ResponseEntity<Map<String, Any>> {
        val count = notificationService.markAllAsRead(userId)
        return ResponseEntity.ok(mapOf(
            "message" to "All notifications marked as read",
            "count" to count
        ))
    }
}
