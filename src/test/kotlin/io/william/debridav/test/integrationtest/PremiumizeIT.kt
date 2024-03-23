package io.william.debridav.test.integrationtest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.william.debridav.DebridApplication
import io.william.debridav.MiltonConfiguration
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.test.integrationtest.config.IntegrationTestContextConfiguration
import io.william.debridav.test.integrationtest.config.MockServerTest
import io.william.debridav.test.integrationtest.config.PremiumizeStubbingService
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
class PremiumizeIT {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var premiumizeStubbingService: PremiumizeStubbingService

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun addingTorrentProducesDebridFileWhenTorrentCached() {
        //given
        val parts = MultipartBodyBuilder()
        parts.part("urls", "magnet")
        parts.part("category", "test")
        parts.part("paused", "false")

        premiumizeStubbingService.mockIsCached()
        premiumizeStubbingService.mockCachedContents()

        //when
        webTestClient.post()
                .uri("/api/v2/torrents/add")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(parts.build()))
                .exchange()
                .expectStatus().is2xxSuccessful
        val debridFile = File("/tmp/debridavtests/downloads/a/b/c/movie.mkv.debridfile")
        val debridFileContents: DebridFileContents = objectMapper.readValue(debridFile)

        //then
        assertTrue(debridFile.exists())
        assertEquals("http://localhost:${premiumizeStubbingService.port}/workingLink", debridFileContents.debridLinks.first().link)
    }
}