package io.william.debrid.premiumize

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.nio.channels.UnresolvedAddressException

@Component
class PremiumizeClient(
    @Value("\${premiumize.apikey}") private val apiKey: String, // = "re8stt9uhcmnxxbf",
    @Value("\${premiumize.baseurl}") private val baseUrl: String // = "https://www.premiumize.me/api/"
) {
    private val logger = LoggerFactory.getLogger(PremiumizeClient::class.java)

    private val restClient = RestClient.create()

    fun isCached(item: String): Boolean {
        try {
            return restClient.get()
                .uri("$baseUrl/cache/check?items[]=$item&apikey=$apiKey")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode::class.java)
                ?.get("response")?.get(0)?.asBoolean() ?: false
        } catch (e: UnresolvedAddressException) {
            logger.error("Failed to check cache for $item")
            throw RuntimeException(e)
        }
    }

    fun getDirectDownloadLink(item: String): DirectDownloadResponse? {
        return restClient.post()
            .uri("$baseUrl/transfer/directdl?apikey=$apiKey&src=${item}")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(DirectDownloadResponse::class.java)
    }

    /*    fun createTransfer(file: MultipartFile) {
            return restClient.post()
                .uri("$baseUrl/transfer/create?apikey=$apiKey")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(DirectDownloadResponse::class.java)
        }*/

}