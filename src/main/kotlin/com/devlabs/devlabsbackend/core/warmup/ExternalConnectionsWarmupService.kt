package com.devlabs.devlabsbackend.core.warmup

import io.minio.MinioClient
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.stereotype.Component

@Component
class ExternalConnectionsWarmupService(
    private val redisConnectionFactory: RedisConnectionFactory,
    private val minioClient: MinioClient
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    fun warmupRedis() {
        logger.info("🔥 Warming up Redis connection...")
        try {
            redisConnectionFactory.connection.use { conn ->
                conn.ping()
            }
            logger.info("✅ Redis connection warmed up")
        } catch (e: Exception) {
            logger.warn("⚠️ Failed to warmup Redis: ${e.message}")
        }
    }
    
    fun warmupMinIO() {
        logger.info("🔥 Warming up MinIO connection...")
        try {
            minioClient.listBuckets()
            logger.info("✅ MinIO connection warmed up")
        } catch (e: Exception) {
            logger.warn("⚠️ Failed to warmup MinIO: ${e.message}")
        }
    }
}
