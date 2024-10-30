package io.william.debridav.debrid.premiumize

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.reflect.*
import io.william.debridav.debrid.CachedFile
import io.william.debridav.debrid.DebridClient
import io.william.debridav.debrid.premiumize.model.CacheCheckResponse
import io.william.debridav.debrid.premiumize.model.DirectDownloadResponse
import io.william.debridav.debrid.premiumize.model.SuccessfulDirectDownloadResponse
import io.william.debridav.debrid.premiumize.model.UnsuccessfulDirectDownloadResponse
import io.william.debridav.fs.DebridProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.nio.channels.UnresolvedAddressException
import java.time.Instant


@Component
@ConditionalOnExpression("#{'\${debridav.debrid-clients}'.contains('premiumize')}")
class PremiumizeClient(
    private val premiumizeConfiguration: PremiumizeConfiguration,
    private val httpClient: HttpClient,
) : DebridClient {

    private val logger = LoggerFactory.getLogger(DebridClient::class.java)

    override suspend fun isCached(magnet: String): Boolean {
        try {
            return httpClient
                .get("${premiumizeConfiguration.baseUrl}/cache/check?items[]=$magnet&apikey=${premiumizeConfiguration.apiKey}")
                .body<CacheCheckResponse>()
                .response.first()
        } catch (e: Exception) {
            logger.error("Failed to check cache for $magnet")
            throw RuntimeException(e)
        }
    }

    override suspend fun getCachedFiles(magnet: String, params: Map<String, String>): List<CachedFile> {
        logger.info("getting cached files from premiumize")
        val resp =
            httpClient.post("${premiumizeConfiguration.baseUrl}/transfer/directdl?apikey=${premiumizeConfiguration.apiKey}&src=$magnet") {
                headers {
                    set(HttpHeaders.ContentType, "multipart/form-data")
                    set(HttpHeaders.Accept, "application/json")
                }
            }.body<DirectDownloadResponse>()

        return when (resp) {
            is SuccessfulDirectDownloadResponse -> {
                return getCachedFilesFromResponse(resp)
            }

            is UnsuccessfulDirectDownloadResponse -> {
                logger.error("Failed to download cached files for $magnet. Status: ${resp.status}")
                listOf()
            }
        }
    }

    private fun getCachedFilesFromResponse(resp: SuccessfulDirectDownloadResponse) =
        resp.content.map {
            CachedFile(
                it.path,
                it.size,
                "video/mp4",
                it.link,
                getProvider(),
                Instant.now().toEpochMilli()
            )
        }

    override fun getProvider(): DebridProvider = DebridProvider.PREMIUMIZE
}