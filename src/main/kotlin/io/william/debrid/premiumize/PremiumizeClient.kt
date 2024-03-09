package io.william.debrid.premiumize

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.multipart.MultipartFile
import java.net.URLEncoder

@Component
class PremiumizeClient {
    private val apiKey = "re8stt9uhcmnxxbf"
    private val baseUrl = "https://www.premiumize.me/api/"
    private val restClient = RestClient.create()

    fun isCached(item: String): Boolean {
        return restClient.get()
            .uri("$baseUrl/cache/check?items[]=$item&apikey=$apiKey")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(JsonNode::class.java)
            ?.get("response")?.get(0)?.asBoolean() ?: false
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