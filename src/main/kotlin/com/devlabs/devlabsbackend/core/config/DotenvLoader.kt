package com.devlabs.devlabsbackend.core.config

import io.github.cdimascio.dotenv.dotenv
import java.io.File

object DotenvLoader {
    private val profile = System.getProperty("spring.profiles.active") ?: "dev"

    fun load() {
        val envFileName = if (File(".env.$profile").exists()) {
            ".env.$profile"
        } else if (File(".env").exists()) {
            ".env"
        } else {
            ".env.dev"
        }

        val dotenv = dotenv {
            filename = envFileName
            ignoreIfMalformed = true
            ignoreIfMissing = true
        }

        dotenv.entries().forEach { entry ->
            System.setProperty(entry.key, entry.value)
        }
    }
}
