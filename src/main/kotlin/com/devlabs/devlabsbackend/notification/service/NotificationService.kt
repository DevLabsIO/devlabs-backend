package com.devlabs.devlabsbackend.notification.service

import com.devlabs.devlabsbackend.core.config.CacheConfig
import com.devlabs.devlabsbackend.core.exception.NotFoundException
import com.devlabs.devlabsbackend.notification.domain.Notification
import com.devlabs.devlabsbackend.notification.domain.dto.CreateNotificationRequest
import com.devlabs.devlabsbackend.notification.domain.dto.NotificationResponse
import com.devlabs.devlabsbackend.notification.domain.dto.UpdateNotificationRequest
import com.devlabs.devlabsbackend.notification.domain.dto.toNotificationResponse
import com.devlabs.devlabsbackend.notification.repository.NotificationRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class NotificationService(
    private val notificationRepository: NotificationRepository
) {

    @Cacheable(value = [CacheConfig.NOTIFICATIONS], key = "'all_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    @Transactional(readOnly = true)
    fun getAllNotifications(pageable: Pageable): Page<NotificationResponse> {
        return notificationRepository.findAll(pageable).map { it.toNotificationResponse() }
    }

    @Cacheable(value = [CacheConfig.NOTIFICATION_DETAIL], key = "#id")
    @Transactional(readOnly = true)
    fun getNotificationById(id: UUID): NotificationResponse {
        val notification = notificationRepository.findById(id).orElseThrow {
            NotFoundException("Notification with id $id not found")
        }
        return notification.toNotificationResponse()
    }

    @Cacheable(
        value = [CacheConfig.NOTIFICATIONS_BY_USER], 
        key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize"
    )
    @Transactional(readOnly = true)
    fun getNotificationsByUser(userId: UUID, pageable: Pageable): Page<NotificationResponse> {
        return notificationRepository.findByCreatedByOrderByCreatedAtDesc(userId, pageable)
            .map { it.toNotificationResponse() }
    }

    @Cacheable(
        value = [CacheConfig.NOTIFICATIONS_UNREAD], 
        key = "#userId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize"
    )
    @Transactional(readOnly = true)
    fun getUnreadNotifications(userId: UUID, pageable: Pageable): Page<NotificationResponse> {
        return notificationRepository.findByCreatedByAndIsViewed(userId, false, pageable)
            .map { it.toNotificationResponse() }
    }

    @Cacheable(value = [CacheConfig.NOTIFICATIONS_UNREAD_COUNT], key = "#userId")
    @Transactional(readOnly = true)
    fun getUnreadCount(userId: UUID): Long {
        return notificationRepository.countByCreatedByAndIsViewed(userId, false)
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.NOTIFICATIONS], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_BY_USER], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_UNREAD], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_UNREAD_COUNT], key = "#request.createdBy")
        ]
    )
    fun createNotification(request: CreateNotificationRequest): NotificationResponse {
        val notification = Notification(
            title = request.title,
            message = request.message,
            createdBy = request.createdBy
        )
        val saved = notificationRepository.save(notification)
        return saved.toNotificationResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.NOTIFICATION_DETAIL], key = "#id"),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_BY_USER], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_UNREAD], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_UNREAD_COUNT], allEntries = true)
        ]
    )
    fun updateNotification(id: UUID, request: UpdateNotificationRequest): NotificationResponse {
        val notification = notificationRepository.findById(id).orElseThrow {
            NotFoundException("Notification with id $id not found")
        }
        notification.isViewed = request.isViewed
        val saved = notificationRepository.save(notification)
        return saved.toNotificationResponse()
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.NOTIFICATION_DETAIL], key = "#id"),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_BY_USER], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_UNREAD], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_UNREAD_COUNT], allEntries = true)
        ]
    )
    fun deleteNotification(id: UUID) {
        val notification = notificationRepository.findById(id).orElseThrow {
            NotFoundException("Notification with id $id not found")
        }
        notificationRepository.delete(notification)
    }

    @Caching(
        evict = [
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_BY_USER], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_UNREAD], allEntries = true),
            CacheEvict(value = [CacheConfig.NOTIFICATIONS_UNREAD_COUNT], key = "#userId")
        ]
    )
    fun markAllAsRead(userId: UUID): Int {
        val unreadNotifications = notificationRepository.findByCreatedByAndIsViewed(
            userId, false, PageRequest.of(0, Int.MAX_VALUE)
        )
        unreadNotifications.forEach { it.isViewed = true }
        notificationRepository.saveAll(unreadNotifications)
        return unreadNotifications.content.size
    }
}
