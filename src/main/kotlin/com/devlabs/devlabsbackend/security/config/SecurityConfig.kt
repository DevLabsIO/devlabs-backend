package com.devlabs.devlabsbackend.security.config

import com.devlabs.devlabsbackend.security.utils.JwtAuthenticationEntryPoint
import com.devlabs.devlabsbackend.security.utils.KeycloakJwtTokenConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
@Profile("dev")
@EnableWebSecurity
@EnableMethodSecurity
class DevSecurityConfig(
    @Autowired private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuerUri: String
) {

    @Bean
    fun jwtDecoderDev(): JwtDecoder {
        val jwkSetUri = "$issuerUri/protocol/openid-connect/certs"
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }

    @Bean
    fun keycloakJwtTokenConverterDev(): KeycloakJwtTokenConverter {
        return KeycloakJwtTokenConverter(JwtGrantedAuthoritiesConverter())
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors(Customizer.withDefaults())
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/api/users/**").permitAll()
                    .requestMatchers("/error", "/actuator/**").permitAll()
                    .requestMatchers("/docs", "/docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .anyRequest().permitAll()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoderDev())
                    jwt.jwtAuthenticationConverter(keycloakJwtTokenConverterDev())
                }
                oauth2.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }
            .exceptionHandling { ex -> 
                ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:3000", "http://172.17.9.74", "https://172.17.9.74")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}

@Configuration
@Profile("prod")
@EnableWebSecurity
@EnableMethodSecurity
class ProdSecurityConfig(
    @Autowired private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri}") private val issuerUri: String
) {

    @Value("\${cors.allowed-origins:}")
    lateinit var allowedOrigins: String

    @Bean
    fun jwtDecoderProd(): JwtDecoder {
        val jwkSetUri = "$issuerUri/protocol/openid-connect/certs"
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }

    @Bean
    fun keycloakJwtTokenConverter(): KeycloakJwtTokenConverter {
        return KeycloakJwtTokenConverter(JwtGrantedAuthoritiesConverter())
    }

    @Bean
    fun prodSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .cors(Customizer.withDefaults())
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/api/users/**").permitAll()
                    .requestMatchers("/error", "/actuator/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(jwtDecoderProd())
                    jwt.jwtAuthenticationConverter(keycloakJwtTokenConverter())
                }
                oauth2.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }
            .exceptionHandling { ex -> 
                ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            }

        return http.build()
    }

    @Bean
    fun prodCorsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        configuration.allowedOrigins = if (allowedOrigins.isBlank()) {
            listOf()
        } else {
            allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf(
            "Authorization", "Content-Type", "X-Requested-With",
            "accept", "Origin", "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        )
        configuration.exposedHeaders = listOf("Authorization")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
