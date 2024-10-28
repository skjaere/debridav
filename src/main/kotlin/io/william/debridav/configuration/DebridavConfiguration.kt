package io.william.debridav.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "debridav")
data class DebridavConfiguration(
    val filePath: String,
    val downloadPath: String,
    val mountPath: String,
    val cacheLocalDebridFilesThreshold: Int
) {
}