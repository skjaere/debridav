package io.william.debridav.debrid.premiumize

import com.fasterxml.jackson.databind.JsonNode
import io.william.debridav.debrid.DebridClient
import io.william.debridav.debrid.DebridLink
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.nio.channels.UnresolvedAddressException


@Component
@ConditionalOnExpression("#{'\${debridav.debridclient}' matches 'premiumize'}")
class PremiumizeClient(
        @Value("\${premiumize.apikey}") private val apiKey: String, // = "re8stt9uhcmnxxbf",
        @Value("\${premiumize.baseurl}") private val baseUrl: String // = "https://www.premiumize.me/api/"
) : DebridClient {
    private val logger = LoggerFactory.getLogger(DebridClient::class.java)

    private val restClient = RestClient.create()

    override fun isCached(magnet: String): Boolean {
        try {
            return restClient.get()
                    .uri("$baseUrl/cache/check?items[]=$magnet&apikey=$apiKey")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(JsonNode::class.java)
                    ?.get("response")?.get(0)?.asBoolean() ?: false
        } catch (e: UnresolvedAddressException) {
            logger.error("Failed to check cache for $magnet")
            throw RuntimeException(e)
        }
    }

    override fun getDirectDownloadLink(magnet: String): List<DebridLink> {
        return restClient.post()
                .uri("$baseUrl/transfer/directdl?apikey=$apiKey&src=${magnet}")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(DirectDownloadResponse::class.java)
                ?.content
                ?.map {
                    DebridLink(
                            it.path,
                            it.size,
                            "video/mp4",
                            it.link
                    )
                } ?: emptyList()
    }
}