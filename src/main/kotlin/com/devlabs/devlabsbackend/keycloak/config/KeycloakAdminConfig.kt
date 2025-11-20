package com.devlabs.devlabsbackend.keycloak.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "keycloak.admin")
class KeycloakAdminConfig {
    lateinit var serverUrl: String
    lateinit var realm: String
    var clientId: String? = null
    var clientSecret: String? = null
    var username: String? = null
    var password: String? = null
}
