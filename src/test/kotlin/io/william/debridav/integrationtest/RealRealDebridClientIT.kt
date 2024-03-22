package io.william.debridav.integrationtest

import io.william.debridav.DebridApplication
import io.william.debridav.MiltonConfiguration
import io.william.debridav.integrationtest.config.IntegrationTestContextConfiguration
import io.william.debridav.integrationtest.config.MockServerTest
import io.william.debridav.integrationtest.config.StubbingService
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.io.File
import java.time.Duration

@SpringBootTest(
        classes = [
            DebridApplication::class,
            IntegrationTestContextConfiguration::class,
            MiltonConfiguration::class
        ],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = [
            "debridav.debridclient=realdebrid",
            "realdebrid.apikey=XLY73JVIYVSJKQHCZ24F2NFGD5LKGGV3XPAV4PQNM34JID4M7OLQ",
            "realdebrid.baseurl=https://api.real-debrid.com/rest/1.0/"

        ]
)
@MockServerTest
class RealDebridClientIT {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var stubbingService: StubbingService

    @Test
    fun realDebrid() {
        //given
        val parts = MultipartBodyBuilder()
        parts.part("urls", "magnet")
        parts.part("category", "test")
        parts.part("paused", "false")

        stubbingService.mockIsCached()
        stubbingService.mockCachedContents()

        //when
        webTestClient
                .mutate().responseTimeout(Duration.ofHours(1)).build()
                .post()
                .uri("/api/v2/torrents/add")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(parts.build()))
                .exchange()
                .expectStatus().is2xxSuccessful

        //then
        assertTrue(File("/tmp/debridavtests/downloads/a/b/c.debridfile").exists())
    }
}