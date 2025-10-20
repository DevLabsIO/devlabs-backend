package com.devlabs.devlabsbackend.notification.repository

import com.devlabs.devlabsbackend.notification.domain.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface NotificationRepository : JpaRepository<Notification, UUID> {
    
    fun findByCreatedBy(createdBy: UUID, pageable: Pageable): Page<Notification>
    
    fun findByCreatedByAndIsViewed(createdBy: UUID, isViewed: Boolean, pageable: Pageable): Page<Notification>
    
    fun countByCreatedByAndIsViewed(createdBy: UUID, isViewed: Boolean): Long
    
    fun findByCreatedByOrderByCreatedAtDesc(createdBy: UUID, pageable: Pageable): Page<Notification>
}
