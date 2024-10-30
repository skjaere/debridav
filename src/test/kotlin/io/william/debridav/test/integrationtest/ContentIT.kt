package io.william.debridav.test.integrationtest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.william.debridav.DebridApplication
import io.william.debridav.MiltonConfiguration
import io.william.debridav.debrid.CachedFile
import io.william.debridav.fs.DebridProvider
import io.william.debridav.test.debridFileContents
import io.william.debridav.test.integrationtest.config.ContentStubbingService
import io.william.debridav.test.integrationtest.config.IntegrationTestContextConfiguration
import io.william.debridav.test.integrationtest.config.MockServerTest
import io.william.debridav.test.integrationtest.config.TestContextInitializer.Companion.BASE_PATH
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import java.io.File
import java.time.Instant

@SpringBootTest(
        classes = [DebridApplication::class, IntegrationTestContextConfiguration::class, MiltonConfiguration::class],
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = ["debridav.debridclient=premiumize"]
)
@MockServerTest
class ContentIT {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var contentStubbingService: ContentStubbingService

    @Test
    fun contentIsServed() {
        //given
        val file = File("$BASE_PATH/testfile.mp4.debridfile")
        val fileContents = debridFileContents.copy()
        fileContents.size = "it works!".toByteArray().size.toLong()
        val debridLink = CachedFile(
            "testfile.mp4",
            link = "http://localhost:${contentStubbingService.port}/workingLink",
            size = "it works!".toByteArray().size.toLong(),
            provider = DebridProvider.PREMIUMIZE,
            lastChecked = Instant.now().toEpochMilli(),
            params = mapOf(),
            mimeType = "video/mp4"
        )
        fileContents.debridLinks = mutableListOf(debridLink)
        //contentStubbingService.mockIsCached()
        contentStubbingService.mockWorkingStream()
        file.writeText(jacksonObjectMapper().writeValueAsString(fileContents))

        //when / then
        webTestClient
                .get()
                .uri("testfile.mp4")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody(String::class.java)
                .isEqualTo("it works!")

        webTestClient.delete()
            .uri("/testfile.mp4")
            .exchange()
            .expectStatus().is2xxSuccessful

    }
}
