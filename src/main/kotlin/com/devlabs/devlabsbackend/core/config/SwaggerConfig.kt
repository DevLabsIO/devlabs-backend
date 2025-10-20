package com.devlabs.devlabsbackend.core.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.models.OpenAPI
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import io.swagger.v3.oas.models.info.Info as OasInfo
import io.swagger.v3.oas.models.info.License as OasLicense

@Configuration
@Profile("dev")
@OpenAPIDefinition(
    info = Info(
        title = "Devlabs API",
        version = "2.0.0",
        description = "Optimized API documentation for the Devlabs Backend",
    ),
    servers = [Server(url = "http://localhost:8090", description = "Local Dev Server")],
)
class SwaggerConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                OasInfo()
                    .title("Devlabs API")
                    .version("2.0.0")
                    .description("Optimized API documentation for the Devlabs Backend")
                    .license(OasLicense().name("Apache 2.0").url("https://springdoc.org"))
            )
    }
}
