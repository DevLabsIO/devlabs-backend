package com.devlabs.devlabsbackend.notification.domain.dto

import com.devlabs.devlabsbackend.notification.domain.Notification
import java.time.Instant
import java.util.*

data class NotificationResponse(
    val id: UUID?,
    val title: String,
    val message: String,
    val isViewed: Boolean,
    val createdBy: UUID,
    val createdAt: Instant
)

data class CreateNotificationRequest(
    val title: String,
    val message: String,
    val createdBy: UUID
)

data class UpdateNotificationRequest(
    val isViewed: Boolean
)

fun Notification.toNotificationResponse(): NotificationResponse {
    return NotificationResponse(
        id = this.id,
        title = this.title,
        message = this.message,
        isViewed = this.isViewed,
        createdBy = this.createdBy,
        createdAt = this.createdAt
    )
}
