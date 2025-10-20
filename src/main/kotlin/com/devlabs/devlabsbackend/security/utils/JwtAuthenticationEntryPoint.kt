package com.devlabs.devlabsbackend.security.utils

import com.devlabs.devlabsbackend.core.logging.logger
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationEntryPoint(private val objectMapper: ObjectMapper) : AuthenticationEntryPoint {
    
    private val log by logger()

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        log.error("Authentication error: ${authException.message}", authException)
        
        val errorMessage = when {
            authException is InvalidBearerTokenException && authException.message?.contains("expired") == true -> {
                "Your session has expired. Please login again."
            }
            authException is InvalidBearerTokenException -> {
                "Invalid authentication token"
            }
            else -> {
                "Authentication failed: ${authException.message}"
            }
        }
        
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = HttpStatus.UNAUTHORIZED.value()
        
        val errorResponse = mapOf(
            "status" to HttpStatus.UNAUTHORIZED.value(),
            "error" to "Unauthorized",
            "message" to errorMessage,
            "path" to request.requestURI
        )
        
        objectMapper.writeValue(response.outputStream, errorResponse)
    }
}
