package io.william.debridav.integrationtest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.william.debridav.DebridApplication
import io.william.debridav.MiltonConfiguration
import io.william.debridav.debridFileContents
import io.william.debridav.fs.DebridLink
import io.william.debridav.fs.DebridProvider
import io.william.debridav.integrationtest.config.IntegrationTestContextConfiguration
import io.william.debridav.integrationtest.config.MockServerTest
import io.william.debridav.integrationtest.config.StubbingService
import io.william.debridav.integrationtest.config.TestContextInitializer.Companion.BASE_PATH
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
class ContentIT {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var stubbingService: StubbingService

    @Test
    fun contentIsServed() {
        stubbingService.mockWorkingStream()

        val file = File("$BASE_PATH/testfile.mp4.debridfile")
        val fileContents = debridFileContents.copy()
        fileContents.debridLinks = mutableListOf(
                DebridLink(DebridProvider.PREMIUMIZE, "http://localhost:${stubbingService.port}/workingLink")
        )
        fileContents.size = "it works!".toByteArray().size.toLong()
        file.writeText(jacksonObjectMapper().writeValueAsString(fileContents))

        webTestClient
                .get()
                .uri("/testfile.mp4")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody(String::class.java)
                .isEqualTo("it works!")

    }
}