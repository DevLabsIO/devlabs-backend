package com.devlabs.devlabsbackend.core.exception

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class SerializationExceptionHandler {

    private val logger = LoggerFactory.getLogger(SerializationExceptionHandler::class.java)

    @ExceptionHandler(HttpMessageNotWritableException::class)
    fun handleHttpMessageNotWritable(ex: HttpMessageNotWritableException): ResponseEntity<SerializationErrorResponse> {
        logger.error("🔥 SERIALIZATION ERROR - HttpMessageNotWritableException", ex)
        
        val rootCause = ex.cause
        val detailedMessage = when (rootCause) {
            is InvalidDefinitionException -> {
                val type = rootCause.type?.rawClass?.simpleName ?: "Unknown"
                val path = rootCause.path.joinToString(" -> ") { it.fieldName ?: it.index.toString() }
                """
                |❌ Cannot serialize type: $type
                |📍 Path: $path
                |💡 Likely causes:
                |   1. Missing Jackson module (check @EnableCaching ObjectMapper config)
                |   2. Hibernate lazy proxy leaked (check if entity was mapped to DTO)
                |   3. Circular reference detected (check for bidirectional relationships)
                |   4. Custom type without serializer (e.g., java.time.Year, custom classes)
                |
                |🔍 Original error: ${rootCause.message}
                """.trimMargin()
            }
            is JsonMappingException -> {
                val path = rootCause.path.joinToString(" -> ") { it.fieldName ?: "[${it.index}]" }
                """
                |❌ JSON Mapping Error
                |📍 Path: $path
                |🔍 Error: ${rootCause.originalMessage}
                |
                |💡 Check the field at this path - it might be:
                |   - A Hibernate proxy (lazy-loaded entity not mapped to DTO)
                |   - An interface type (return concrete class instead)
                |   - Missing getter/setter
                """.trimMargin()
            }
            is JsonProcessingException -> {
                """
                |❌ JSON Processing Error
                |🔍 Error: ${rootCause.originalMessage}
                |📍 Location: Line ${rootCause.location?.lineNr}, Column ${rootCause.location?.columnNr}
                """.trimMargin()
            }
            else -> {
                """
                |❌ Generic Serialization Error
                |🔍 Error: ${ex.message}
                |🔍 Root cause: ${rootCause?.message ?: "Unknown"}
                |
                |💡 Check server logs for full stack trace
                """.trimMargin()
            }
        }

        logger.error("📝 Detailed error:\n$detailedMessage")
        
        if (rootCause != null) {
            logger.error("🔍 Root cause stack trace:", rootCause)
        }

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                SerializationErrorResponse(
                    error = "Serialization Error",
                    message = detailedMessage,
                    statusCode = 500,
                    timestamp = LocalDateTime.now().toString(),
                    hint = getHint(rootCause)
                )
            )
    }

    @ExceptionHandler(InvalidDefinitionException::class)
    fun handleInvalidDefinition(ex: InvalidDefinitionException): ResponseEntity<SerializationErrorResponse> {
        logger.error("🔥 SERIALIZATION ERROR - InvalidDefinitionException", ex)
        
        val type = ex.type?.rawClass?.name ?: "Unknown"
        val simpleName = ex.type?.rawClass?.simpleName ?: "Unknown"
        val path = ex.path.joinToString(" -> ") { it.fieldName ?: it.index.toString() }
        
        val detailedMessage = """
            |❌ Cannot serialize type: $simpleName
            |📍 Full type: $type
            |📍 Path: $path
            |🔍 Error: ${ex.message}
            |
            |💡 Common fixes:
            |   1. Add custom serializer in CacheConfig for this type
            |   2. Map entity to DTO (don't return JPA entities)
            |   3. Add @JsonIgnore to problematic field
            |   4. Register Jackson module for this type
        """.trimMargin()

        logger.error("📝 Detailed error:\n$detailedMessage")

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                SerializationErrorResponse(
                    error = "Invalid Type Definition",
                    message = detailedMessage,
                    statusCode = 500,
                    timestamp = LocalDateTime.now().toString(),
                    hint = "Check CacheConfig.kt - add custom serializer for $simpleName"
                )
            )
    }

    @ExceptionHandler(JsonMappingException::class)
    fun handleJsonMapping(ex: JsonMappingException): ResponseEntity<SerializationErrorResponse> {
        logger.error("🔥 SERIALIZATION ERROR - JsonMappingException", ex)
        
        val path = ex.path.joinToString(" -> ") { it.fieldName ?: "[${it.index}]" }
        
        val detailedMessage = """
            |❌ JSON Mapping Error
            |📍 Path: $path
            |🔍 Error: ${ex.originalMessage}
            |
            |💡 Likely causes:
            |   1. Circular reference (A -> B -> A)
            |   2. Lazy-loaded collection not initialized
            |   3. Proxy object leaked into response
        """.trimMargin()

        logger.error("📝 Detailed error:\n$detailedMessage")

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                SerializationErrorResponse(
                    error = "JSON Mapping Error",
                    message = detailedMessage,
                    statusCode = 500,
                    timestamp = LocalDateTime.now().toString(),
                    hint = "Use DTOs instead of entities. Check path: $path"
                )
            )
    }

    private fun getHint(cause: Throwable?): String {
        return when {
            cause?.message?.contains("hibernate", ignoreCase = true) == true -> 
                "Hibernate proxy detected! Map your entity to a DTO before returning."
            
            cause?.message?.contains("Optional", ignoreCase = true) == true -> 
                "Don't return Optional<T> directly. Return T or use .orElse(null)"
            
            cause?.message?.contains("java.time", ignoreCase = true) == true -> 
                "Java Time type issue. Check if JavaTimeModule is registered in CacheConfig."
            
            cause?.message?.contains("Year", ignoreCase = true) == true -> 
                "java.time.Year serialization issue. Check yearModule in CacheConfig."
            
            cause?.message?.contains("no properties", ignoreCase = true) == true -> 
                "Class has no serializable properties. Add getters or make it a data class."
            
            cause?.message?.contains("circular", ignoreCase = true) == true -> 
                "Circular reference detected. Use @JsonIgnore or break the cycle in DTOs."
            
            else -> "Check server logs for full stack trace with line numbers."
        }
    }
}

data class SerializationErrorResponse(
    val error: String,
    val message: String,
    val statusCode: Int,
    val timestamp: String,
    val hint: String
)