package io.william.debridav.integrationtest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.william.debridav.DebridApplication
import io.william.debridav.MiltonConfiguration
import io.william.debridav.integrationtest.config.IntegrationTestContextConfiguration
import io.william.debridav.integrationtest.config.MockServerTest
import io.william.debridav.integrationtest.config.StubbingService
import io.william.debridav.qbittorrent.TorrentsInfoResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.io.File

@SpringBootTest(
        classes = [DebridApplication::class, IntegrationTestContextConfiguration::class, MiltonConfiguration::class],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = ["debridav.debridclient=premiumize"]
)
@MockServerTest
class TorrentEmulationIT {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var stubbingService: StubbingService

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun addingTorrentProducesDebridFileWhenTorrentCached() {
        //given
        val parts = MultipartBodyBuilder()
        parts.part("urls", "magnet")
        parts.part("category", "test")
        parts.part("paused", "false")

        stubbingService.mockIsCached()
        stubbingService.mockCachedContents()

        //when
        webTestClient.post()
                .uri("/api/v2/torrents/add")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(parts.build()))
                .exchange()
                .expectStatus().is2xxSuccessful

        //then
        assertTrue(File("/tmp/debridavtests/downloads/a/b/c.debridfile").exists())
    }

    @Test
    fun torrentsInfoEndpointPointsToCorrectLocation() {
        //given
        val parts = MultipartBodyBuilder()
        parts.part("urls", "magnet")
        parts.part("category", "test")
        parts.part("paused", "false")

        stubbingService.mockIsCached()
        stubbingService.mockCachedContents()

        //when
        webTestClient.post()
                .uri("/api/v2/torrents/add")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(parts.build()))
                .exchange()
                .expectStatus().is2xxSuccessful

        //then
        val type = objectMapper.typeFactory.constructCollectionType(List::class.java, TorrentsInfoResponse::class.java)
        val torrentsInfoResponse = webTestClient.get()
                .uri("/api/v2/torrents/info?category=test")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody(String::class.java)
                .returnResult().responseBody
        val parsedResponse: List<TorrentsInfoResponse> = objectMapper.readValue(torrentsInfoResponse, type)

        assertEquals("/mnt/localdav/downloads/a", parsedResponse.first().contentPath)
    }
}
