package com.devlabs.devlabsbackend.core.warmup

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import javax.sql.DataSource

@Component
class DatabaseWarmupService(
    private val dataSource: DataSource
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun warmupConnectionPool() {
        logger.info("🔥 Warming up database connection pool...")
        val startTime = System.currentTimeMillis()
        
        try {
            repeat(10) {
                dataSource.connection.use { conn ->
                    conn.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT 1").use { rs ->
                            rs.next()
                        }
                    }
                }
            }
            
            val duration = System.currentTimeMillis() - startTime
            logger.info("✅ Database connection pool warmed up in ${duration}ms")
        } catch (e: Exception) {
            logger.error("❌ Failed to warmup connection pool", e)
        }
    }
}
