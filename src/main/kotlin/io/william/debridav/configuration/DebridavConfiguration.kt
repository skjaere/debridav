package io.william.debridav.configuration

import io.william.debridav.fs.DebridProvider
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "debridav")
data class DebridavConfiguration(
    val filePath: String,
    val downloadPath: String,
    val mountPath: String,
    val cacheLocalDebridFilesThresholdMb: Int,
    val debridClients: List<DebridProvider>
) {
}