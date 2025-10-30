package com.devlabs.devlabsbackend.core.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration
import java.time.Year

@Configuration
@EnableCaching
class CacheConfig : CachingConfigurer {

    private val logger = org.slf4j.LoggerFactory.getLogger(CacheConfig::class.java)

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.devlabs.devlabsbackend.")  // Allow all our DTOs (note the trailing dot!)
            .allowIfSubType("java.util.")
            .allowIfSubType("java.time.")
            .allowIfSubType("java.sql.")  // For Timestamp
            .allowIfSubType("kotlin.collections.")
            .build()

        val yearModule = SimpleModule().apply {
            addSerializer(Year::class.java, object : JsonSerializer<Year>() {
                override fun serialize(value: Year?, gen: JsonGenerator, serializers: SerializerProvider) {
                    if (value == null) {
                        gen.writeNull()
                    } else {
                        gen.writeNumber(value.value)
                    }
                }

                override fun serializeWithType(
                    value: Year?,
                    gen: JsonGenerator,
                    serializers: SerializerProvider,
                    typeSer: com.fasterxml.jackson.databind.jsontype.TypeSerializer
                ) {
                    // Write the type wrapper and then the value
                    val writableTypeId = typeSer.typeId(value, Year::class.java, JsonToken.VALUE_NUMBER_INT)
                    typeSer.writeTypePrefix(gen, writableTypeId)
                    serialize(value, gen, serializers)
                    typeSer.writeTypeSuffix(gen, writableTypeId)
                }
            })
            addDeserializer(Year::class.java, object : JsonDeserializer<Year>() {
                override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Year {
                    return Year.of(p.intValue)
                }
            })
        }

        val objectMapper = ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .registerModule(yearModule)
            .activateDefaultTyping(
                ptv,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
            )
        
        val serializer = GenericJackson2JsonRedisSerializer(objectMapper)

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(serializer)
            )
            .disableCachingNullValues()

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .withCacheConfiguration(USERS_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(USERS_LIST_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(USER_DETAIL_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(DEPARTMENT_DETAIL_CACHE, defaultConfig.entryTtl(Duration.ofHours(2)))
            .withCacheConfiguration(DEPARTMENTS_LIST_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(DEPARTMENTS_CACHE, defaultConfig.entryTtl(Duration.ofHours(2)))
            .withCacheConfiguration(BATCH_DETAIL_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(BATCHES_LIST_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(BATCHES_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(BATCH_STUDENTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(SEMESTER_DETAIL_CACHE, defaultConfig.entryTtl(Duration.ofHours(2)))
            .withCacheConfiguration(SEMESTERS_LIST_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(SEMESTERS_CACHE, defaultConfig.entryTtl(Duration.ofHours(2)))
            .withCacheConfiguration(COURSE_DETAIL_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(COURSES_LIST_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(COURSES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(COURSES_ACTIVE_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration(COURSES_USER_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(20)))
            .withCacheConfiguration(COURSE_INSTRUCTORS_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(COURSE_STUDENTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(COURSE_BATCHES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(COURSE_PERFORMANCE_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration(TEAM_DETAIL_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(TEAMS_LIST, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(PROJECT_DETAIL, defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(PROJECTS_LIST, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(PROJECTS_ARCHIVE_CACHE, defaultConfig.entryTtl(Duration.ofHours(2)))
            .withCacheConfiguration(PROJECT_REVIEWS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration(RUBRICS_DETAIL_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration(RUBRICS_LIST_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(REVIEWS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration(REVIEW_DETAIL_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration(RUBRICS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(INDIVIDUAL_SCORES_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration(KANBAN_BOARD, defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration(KANBAN_TASK, defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration(DASHBOARD_ADMIN, defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration(DASHBOARD_MANAGER, defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration(DASHBOARD_STUDENT, defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration(NOTIFICATIONS, defaultConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration(NOTIFICATION_DETAIL, defaultConfig.entryTtl(Duration.ofMinutes(10)))
            .withCacheConfiguration(NOTIFICATIONS_BY_USER, defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration(NOTIFICATIONS_UNREAD, defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration(NOTIFICATIONS_UNREAD_COUNT, defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration(EVALUATION_DRAFTS_CACHE, defaultConfig.entryTtl(Duration.ofHours(24)))
            .withCacheConfiguration(BLOB_METADATA_CACHE, defaultConfig.entryTtl(Duration.ofHours(1)))
            .build()
    }

    @Bean
    override fun errorHandler(): CacheErrorHandler {
        return object : CacheErrorHandler {
            override fun handleCachePutError(ex: RuntimeException, cache: Cache, key: Any, value: Any?) {
                val valueType = value?.let { it::class.qualifiedName } ?: "null"
                logger.error("Cache PUT failed for cache='${cache.name}', key=$key, valueType=$valueType", ex)

                if (value != null) {
                    logger.error("📦 Failed value: ${value.toString().take(500)}")
                }
            }

            override fun handleCacheGetError(ex: RuntimeException, cache: Cache, key: Any) {
                logger.error("Cache GET failed for cache='${cache.name}', key=$key", ex)

            }

            override fun handleCacheEvictError(ex: RuntimeException, cache: Cache, key: Any) {
                logger.error("Cache EVICT failed for cache='${cache.name}', key=$key", ex)
            }

            override fun handleCacheClearError(ex: RuntimeException, cache: Cache) {
                logger.error("Cache CLEAR failed for cache='${cache.name}'", ex)
            }
        }
    }

    companion object {
        const val USERS_CACHE = "users"
        const val USERS_LIST_CACHE = "users-list"
        const val USER_DETAIL_CACHE = "user-detail"
        const val DEPARTMENT_DETAIL_CACHE = "department-detail"
        const val DEPARTMENTS_LIST_CACHE = "departments-list"
        const val DEPARTMENTS_CACHE = "departments"
        const val BATCH_DETAIL_CACHE = "batch-detail"
        const val BATCHES_LIST_CACHE = "batches-list"
        const val BATCHES_CACHE = "batches"
        const val BATCH_STUDENTS_CACHE = "batch-students"
        const val SEMESTER_DETAIL_CACHE = "semester-detail"
        const val SEMESTERS_LIST_CACHE = "semesters-list"
        const val SEMESTERS_CACHE = "semesters"
        const val COURSE_DETAIL_CACHE = "course-detail"
        const val COURSES_LIST_CACHE = "courses-list"
        const val COURSES_CACHE = "courses"
        const val COURSES_ACTIVE_CACHE = "courses-active"
        const val COURSES_USER_CACHE = "courses-user"
        const val COURSE_INSTRUCTORS_CACHE = "course-instructors"
        const val COURSE_STUDENTS_CACHE = "course-students"
        const val COURSE_BATCHES_CACHE = "course-batches"
        const val COURSE_PERFORMANCE_CACHE = "course-performance"
        const val TEAM_DETAIL_CACHE = "team-detail"
        const val TEAMS_LIST = "teams-list"
        const val PROJECT_DETAIL = "project-detail"
        const val PROJECTS_LIST = "projects-list"
        const val PROJECTS_ARCHIVE_CACHE = "projects-archive"
        const val PROJECT_REVIEWS_CACHE = "project-reviews"
        const val RUBRICS_DETAIL_CACHE = "rubrics-detail"
        const val RUBRICS_LIST_CACHE = "rubrics-list"
        const val REVIEWS_CACHE = "reviews"
        const val REVIEW_DETAIL_CACHE = "review-detail"
        const val RUBRICS_CACHE = "rubrics"
        const val INDIVIDUAL_SCORES_CACHE = "individual-scores"
        const val KANBAN_BOARD = "kanban-board"
        const val KANBAN_TASK = "kanban-task"
        const val DASHBOARD_ADMIN = "dashboard-admin"
        const val DASHBOARD_MANAGER = "dashboard-manager"
        const val DASHBOARD_STUDENT = "dashboard-student"
        const val NOTIFICATIONS = "notifications"
        const val NOTIFICATION_DETAIL = "notification-detail"
        const val NOTIFICATIONS_BY_USER = "notifications-by-user"
        const val NOTIFICATIONS_UNREAD = "notifications-unread"
        const val NOTIFICATIONS_UNREAD_COUNT = "notifications-unread-count"
        const val EVALUATION_DRAFTS_CACHE = "evaluation-drafts"
        const val BLOB_METADATA_CACHE = "blob-metadata"
    }
}
