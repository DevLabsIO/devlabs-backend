package com.devlabs.devlabsbackend.core.config

import io.github.cdimascio.dotenv.dotenv

object DotenvLoader {
    private val profile = System.getProperty("spring.profiles.active") ?: "dev";    fun load() {
        val dotenv = dotenv {
            filename = ".env"
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }

        dotenv.entries().forEach { entry ->
            System.setProperty(entry.key, entry.value)
        }
    }
}
