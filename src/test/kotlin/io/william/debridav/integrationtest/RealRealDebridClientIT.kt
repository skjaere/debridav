package io.william.debridav.integrationtest

import io.william.debridav.DebridApplication
import io.william.debridav.MiltonConfiguration
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
//@MockServerTest
class RealDebridClientIT {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun realDebrid() {
        //given
        val parts = MultipartBodyBuilder()
        parts.part("urls", "magnet:?xt=urn:btih:0AF956F0DF28CA848BBBB2E29BB02A2D1F16BE31&dn=MasterChef+US+S13E01+1080p+WEB+h264-BAE&tr=http%3a%2f%2ftracker.opentrackr.org%3a1337%2fannounce&tr=udp%3a%2f%2ftracker.auctor.tv%3a6969%2fannounce&tr=udp%3a%2f%2fopentracker.i2p.rocks%3a6969%2fannounce&tr=https%3a%2f%2fopentracker.i2p.rocks%3a443%2fannounce&tr=udp%3a%2f%2fopen.demonii.com%3a1337%2fannounce&tr=udp%3a%2f%2ftracker.openbittorrent.com%3a6969%2fannounce&tr=http%3a%2f%2ftracker.openbittorrent.com%3a80%2fannounce&tr=udp%3a%2f%2fopen.stealth.si%3a80%2fannounce&tr=udp%3a%2f%2ftracker.torrent.eu.org%3a451%2fannounce&tr=udp%3a%2f%2ftracker.moeking.me%3a6969%2fannounce&tr=udp%3a%2f%2fexplodie.org%3a6969%2fannounce&tr=udp%3a%2f%2fexodus.desync.com%3a6969%2fannounce&tr=udp%3a%2f%2fuploads.gamecoast.net%3a6969%2fannounce&tr=udp%3a%2f%2ftracker1.bt.moack.co.kr%3a80%2fannounce&tr=udp%3a%2f%2ftracker.tiny-vps.com%3a6969%2fannounce&tr=udp%3a%2f%2ftracker.theoks.net%3a6969%2fannounce&tr=udp%3a%2f%2ftracker.skyts.net%3a6969%2fannounce&tr=udp%3a%2f%2ftracker-udp.gbitt.info%3a80%2fannounce&tr=udp%3a%2f%2fopen.tracker.ink%3a6969%2fannounce&tr=udp%3a%2f%2fmovies.zsw.ca%3a6969%2fannounce")
        parts.part("category", "test")
        parts.part("paused", "false")

        //stubbingService.mockIsCached()
        //stubbingService.mockCachedContents()

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