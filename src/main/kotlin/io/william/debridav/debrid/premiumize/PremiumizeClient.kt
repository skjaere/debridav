package io.william.debridav.debrid.premiumize

import com.fasterxml.jackson.databind.JsonNode
import io.william.debridav.debrid.DebridClient
import io.william.debridav.debrid.DebridResponse
import io.william.debridav.fs.DebridProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.nio.channels.UnresolvedAddressException


@Component
@ConditionalOnExpression("#{'\${debridav.debridclient}' matches 'premiumize'}")
class PremiumizeClient(private val premiumizeConfiguration: PremiumizeConfiguration) : DebridClient {

    private val logger = LoggerFactory.getLogger(DebridClient::class.java)

    private val restClient = RestClient.create()

    override fun isCached(magnet: String): Boolean {
        try {
            return restClient.get()
                    .uri("${premiumizeConfiguration.baseUrl}/cache/check?items[]=$magnet&apikey=${premiumizeConfiguration.apiKey}")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode::class.java)
                    ?.get("response")?.get(0)?.asBoolean() ?: false
        } catch (e: UnresolvedAddressException) {
            logger.error("Failed to check cache for $magnet")
            throw RuntimeException(e)
        }
    }

    override fun getDirectDownloadLink(magnet: String): List<DebridResponse> {
        return restClient.post()
                .uri("${premiumizeConfiguration.baseUrl}/transfer/directdl?apikey=${premiumizeConfiguration.apiKey}&src=$magnet")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(DirectDownloadResponse::class.java)
                ?.content
                ?.map {
                    DebridResponse(
                            it.path,
                            it.size,
                            "video/mp4",
                            it.link
                    )
                } ?: emptyList()
    }

    override fun getProvider(): DebridProvider = DebridProvider.PREMIUMIZE
}