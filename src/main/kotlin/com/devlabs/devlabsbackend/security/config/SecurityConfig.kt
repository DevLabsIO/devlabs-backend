package com.devlabs.devlabsbackend.security.config

import com.devlabs.devlabsbackend.security.utils.JwtAuthenticationEntryPoint
import com.devlabs.devlabsbackend.security.utils.KeycloakJwtTokenConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@Profile("dev")
@EnableWebSecurity
@EnableMethodSecurity
class DevSecurityConfig(@Autowired private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint) {

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
                    .requestMatchers("/api/user/check-exists").permitAll()
                    .requestMatchers("/error", "/actuator/**").permitAll()
                    .anyRequest().permitAll()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
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
        configuration.allowedOrigins = listOf("http://localhost:3000")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
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
class SecurityConfig(@Autowired private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint) {

    @Value("\${cors.allowed-origins:}")
    lateinit var allowedOrigins: String

    @Bean
    fun keycloakJwtTokenConverter(): KeycloakJwtTokenConverter {
        return KeycloakJwtTokenConverter(JwtGrantedAuthoritiesConverter())
    }

    @Bean
    @Throws(Exception::class)
    fun devlabsServerFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.disable() }
            .cors(Customizer.withDefaults())
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }            .authorizeHttpRequests { authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/api/users/**").permitAll()
                    .requestMatchers("/api/user/check-exists").permitAll()
                    .requestMatchers("/error", "/actuator/**").permitAll()
                 .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
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
    fun corsConfigurationSource(): CorsConfigurationSource {
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
