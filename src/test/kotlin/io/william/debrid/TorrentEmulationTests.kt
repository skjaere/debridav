package io.william.debrid

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
    classes = [DebridApplication::class, IntegrationTestContextConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@MockServerTest
class TorrentEmulationTests {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var stubbingService: StubbingService

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
}
