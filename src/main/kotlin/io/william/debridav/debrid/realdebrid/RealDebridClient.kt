package io.william.debridav.debrid.realdebrid

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.william.debridav.debrid.DebridClient
import io.william.debridav.debrid.DebridLink
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.nio.channels.UnresolvedAddressException


@Component
@ConditionalOnExpression("#{'\${debridav.debridclient}' matches 'realdebrid'}")
class RealDebridClient(
        @Value("\${realdebrid.apikey}") private val apiKey: String,
        @Value("\${realdebrid.baseurl}") private val baseUrl: String
) : DebridClient {
    private val logger = LoggerFactory.getLogger(RealDebridClient::class.java)
    private val restClient = RestClient.create()

    override fun isCached(magnet: String): Boolean {
        try {
            val hash = MagnetParser.getHashFromMagnet(magnet)
            return (restClient.get()
                    .uri("$baseUrl/torrents/instantAvailability/$hash")
                    .accept(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                    .retrieve()
                    .body(JsonNode::class.java)
                    ?.get(hash!!.lowercase())?.get("rd")?.size() ?: -1) > 0
        } catch (e: UnresolvedAddressException) {
            logger.error("Failed to check cache for $magnet")
            throw RuntimeException(e)
        } catch (e: Exception) {
            logger.error("something went wrong", e)
            throw RuntimeException(e)
        }
    }

    override fun getDirectDownloadLink(magnet: String): List<DebridLink> {
        return addMagnet(magnet)?.let { addMagnetResponse ->
            selectFilesFromTorrent(addMagnetResponse.id)
            getTorrentInfo(addMagnetResponse.id).let { hostedFiles ->
                hostedFiles
                        .mapNotNull { unrestrictLink(it.link) }
                        .map { unrestrictedLink ->
                            DebridLink(
                                    unrestrictedLink.filename,
                                    unrestrictedLink.filesize,
                                    unrestrictedLink.mimeType,
                                    unrestrictedLink.download
                            )
                        }
            }
        } ?: emptyList()
    }

    data class AddMagnetResponse(
            val id: String,
            val uri: String
    )

    private fun addMagnet(magnet: String): AddMagnetResponse? {
        val response = restClient.post()
                .uri("$baseUrl/torrents/addMagnet")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .body("magnet=$magnet")
                .retrieve()
                .body(AddMagnetResponse::class.java)

        return response
    }

    data class HostedFile(
            val fileId: String,
            val fileName: String,
            val fileSize: Long,
            val link: String
    )

    private fun getTorrentInfo(id: String): List<HostedFile> {
        val response = restClient.get()
                .uri("$baseUrl/torrents/info/$id")
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .retrieve()
                .body(JsonNode::class.java)

        return response?.let {
            it as ObjectNode
            it["files"].mapIndexed { idx, file ->
                HostedFile(
                        file["id"].asText(),
                        file["path"].asText(),
                        file["bytes"].asLong(),
                        it["links"][idx].asText()
                )
            }
        }?.toList() ?: emptyList()
    }

    private fun selectFilesFromTorrent(torrentId: String) {
        restClient.post()
                .uri("$baseUrl/torrents/selectFiles/$torrentId")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .body("files=all")
                .retrieve()
                .body(AddMagnetResponse::class.java)
    }


    data class UnrestrictedLink(
            val id: String,
            val filename: String,
            val mimeType: String,
            val filesize: Long,
            val link: String,
            val host: String,
            val download: String
    )

    private fun unrestrictLink(link: String): UnrestrictedLink? {
        return restClient.post()
                .uri("$baseUrl/unrestrict/link")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $apiKey")
                .body("link=$link")
                .retrieve()
                .body(UnrestrictedLink::class.java)
    }
}