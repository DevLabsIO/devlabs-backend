package com.devlabs.devlabsbackend.core.warmup

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ApplicationWarmupService(
    private val jpaWarmupService: JpaWarmupService,
    private val databaseWarmupService: DatabaseWarmupService,
    private val externalConnectionsWarmupService: ExternalConnectionsWarmupService,
    private val repositoryWarmupService: RepositoryWarmupService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @EventListener(ApplicationReadyEvent::class)
    @Async
    fun warmupApplication() {
        logger.info("🚀 Starting application warmup...")
        val overallStart = System.currentTimeMillis()
        
        try {
            databaseWarmupService.warmupConnectionPool()
            jpaWarmupService.warmupJpaEntities()
            repositoryWarmupService.warmupRepositories()
            externalConnectionsWarmupService.warmupRedis()
            externalConnectionsWarmupService.warmupMinIO()
            
            val totalDuration = System.currentTimeMillis() - overallStart
            logger.info("🎉 Application warmup completed successfully in ${totalDuration}ms")
        } catch (e: Exception) {
            logger.error("❌ Application warmup failed", e)
        }
    }
}
