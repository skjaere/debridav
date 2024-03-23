package io.william.debridav.test.integrationtest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.william.debridav.DebridApplication
import io.william.debridav.MiltonConfiguration
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.fs.DebridLink
import io.william.debridav.fs.DebridProvider
import io.william.debridav.test.debridFileContents
import io.william.debridav.test.integrationtest.config.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import java.io.File

@SpringBootTest(
        classes = [DebridApplication::class, IntegrationTestContextConfiguration::class, MiltonConfiguration::class],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = ["debridav.debridclient=premiumize"]
)
@MockServerTest
class MissingDebridProviderIT {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var premiumizeStubbingService: PremiumizeStubbingService

    @Autowired
    private lateinit var contentStubbingService: ContentStubbingService

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun providerGetsAddedToDebridFileWhenMissing() {
        //given
        contentStubbingService.mockWorkingStream()
        premiumizeStubbingService.mockIsCached()
        premiumizeStubbingService.mockCachedContents()

        val file = File("${TestContextInitializer.BASE_PATH}/testfile.mp4.debridfile")
        val fileContents = debridFileContents.copy()
        fileContents.debridLinks = mutableListOf(
                DebridLink(DebridProvider.REAL_DEBRID, "http://localhost:${premiumizeStubbingService.port}/realDebridLink")
        )
        fileContents.size = "it works!".toByteArray().size.toLong()
        file.writeText(jacksonObjectMapper().writeValueAsString(fileContents))

        //when
        webTestClient
                .get()
                .uri("/testfile.mp4")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody(String::class.java)
                .isEqualTo("it works!")
        val debridFileContents: DebridFileContents = objectMapper.readValue(file)

        //then
        assertEquals(2, debridFileContents.debridLinks.size)
        assertEquals("http://localhost:${premiumizeStubbingService.port}/workingLink", debridFileContents.debridLinks.first { it.provider == DebridProvider.PREMIUMIZE }.link)
    }

    @Test
    fun providerGetsAddedToDebridFileWhenMissingAndPathIsDifferent() {
        //given
        contentStubbingService.mockWorkingStream()
        premiumizeStubbingService.mockIsCached()
        premiumizeStubbingService.mockCachedContents()

        val file = File("${TestContextInitializer.BASE_PATH}/testfile.mp4.debridfile")
        val fileContents = debridFileContents.copy()
        fileContents.debridLinks = mutableListOf(
                DebridLink(DebridProvider.REAL_DEBRID, "http://localhost:${premiumizeStubbingService.port}/realDebridLink")
        )
        fileContents.originalPath = "movie.mkv"
        fileContents.size = "it works!".toByteArray().size.toLong()
        file.writeText(jacksonObjectMapper().writeValueAsString(fileContents))

        //when
        webTestClient
                .get()
                .uri("/testfile.mp4")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody(String::class.java)
                .isEqualTo("it works!")
        val debridFileContents: DebridFileContents = objectMapper.readValue(file)

        //then
        assertEquals(2, debridFileContents.debridLinks.size)
        assertEquals("http://localhost:${premiumizeStubbingService.port}/workingLink", debridFileContents.debridLinks.first { it.provider == DebridProvider.PREMIUMIZE }.link)
    }
}