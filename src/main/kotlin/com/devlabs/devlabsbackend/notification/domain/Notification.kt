package com.devlabs.devlabsbackend.notification.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(
    name = "notification",
    indexes = [
        Index(name = "idx_notification_created_by", columnList = "createdBy"),
        Index(name = "idx_notification_is_viewed", columnList = "isViewed"),
        Index(name = "idx_notification_created_at", columnList = "createdAt")
    ]
)
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    
    val title: String,
    
    @Column(columnDefinition = "TEXT")
    val message: String,
    
    var isViewed: Boolean = false,
    
    val createdBy: UUID,
    
    val createdAt: Instant = Instant.now()
)
