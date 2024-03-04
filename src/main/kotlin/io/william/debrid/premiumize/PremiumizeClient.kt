package io.william.debrid.premiumize

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.net.URLEncoder

class PremiumizeClient {
    private val apiKey = "re8stt9uhcmnxxbf"
    private val baseUrl = "https://www.premiumize.me/api/"
    private val restClient = RestClient.create()

    fun isCached(item: String): Boolean {
        return restClient.get()
            .uri("$baseUrl/cache/check?items=$item&apikey=$apiKey")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve().toEntity(JsonNode::class.java)
            .body
            ?.asBoolean() ?: false
    }

    fun getDirectDownloadLink(item: String): DirectDownloadResponse? {
        return restClient.post()
            .uri("$baseUrl/transfer/directdl?apikey=$apiKey&src=${item}")
            .body("src=${URLEncoder.encode(item, Charsets.UTF_8)}")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve().body(DirectDownloadResponse::class.java)

    }

}