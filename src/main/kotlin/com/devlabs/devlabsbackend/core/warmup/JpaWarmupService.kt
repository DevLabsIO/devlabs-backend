package com.devlabs.devlabsbackend.core.warmup

import jakarta.persistence.EntityManager
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class JpaWarmupService(
    private val entityManager: EntityManager
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun warmupJpaEntities() {
        logger.info("🔥 Warming up JPA entities...")
        val startTime = System.currentTimeMillis()
        
        try {
            entityManager.metamodel.entities.forEach { entityType ->
                try {
                    val javaType = entityType.javaType
                    val query = entityManager.createQuery(
                        "SELECT e FROM ${entityType.name} e", 
                        javaType
                    )
                    query.maxResults = 1
                    query.resultList
                } catch (e: Exception) {
                    logger.warn("Failed to warmup entity ${entityType.name}: ${e.message}")
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            logger.info("✅ JPA entities warmed up in ${duration}ms")
        } catch (e: Exception) {
            logger.error("❌ Failed to warmup JPA entities", e)
        }
    }
}
