package io.william.debrid

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.william.debrid.fs.FileService
import io.william.debrid.fs.models.DebridFileContents
import io.william.debrid.premiumize.PremiumizeClient
import io.william.debrid.resource.DebridFileResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.ByteArrayOutputStream
import java.io.File


@MockServerTest
@SpringBootTest
class FileServiceDeadLinkTests {
    private val objectMapper = jacksonObjectMapper()

    @Autowired
    private lateinit var stubbingService: StubbingService

    @Test
    fun servesRefreshedStreamWhenEncountering404() {
        //given

        stubbingService.mockDeadLink()
        stubbingService.mockIsCached()
        stubbingService.mockCachedContents()
        stubbingService.mockWorkingStream()


        val premiumizeClient = PremiumizeClient(
            "abd", "http://localhost:${stubbingService.port}"
        )
        val streamingService = StreamingService()
        val fileService = FileService(
            premiumizeClient,
            "/tmp/debridtest/files",
            2,
            streamingService
        )
        val debridFile = File("/tmp/debridtest")
        val debridFileContents = DebridFileContents(
            "a/b/c",
            100,
            1000,
            "http://localhost:${stubbingService.port}/deadLink",
            "magnet"
        )

        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileResource = DebridFileResource(
            debridFile, fileService, streamingService
        )

        //when
        val outputStream = ByteArrayOutputStream()
        fileResource.sendContent(
            outputStream, null, null, null
        )

        //then
        val result = outputStream.toByteArray().toString(Charsets.UTF_8)
        assertEquals(result, "it works!")
    }

    @Test
    fun deletesFileWhenLinkCannotBeRefreshed() {
        //given
        stubbingService.mockIsNotCached()
        stubbingService.mockDeadLink()


        val premiumizeClient = PremiumizeClient(
            "abd", "http://localhost:${stubbingService.port}"
        )
        val streamingService = StreamingService()
        val fileService = FileService(
            premiumizeClient,
            "/tmp/debridtest/files",
            2,
            streamingService
        )
        val debridFile = File("/tmp/debridtest")
        val debridFileContents = DebridFileContents(
            "a/b/c",
            100,
            1000,
            "http://localhost:${stubbingService.port}/deadLink",
            "magnet"
        )
        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileResource = DebridFileResource(
            debridFile, fileService, streamingService
        )

        //when
        val outputStream = ByteArrayOutputStream()
        fileResource.sendContent(
            outputStream, null, null, null
        )

        //then
        val result = outputStream.toByteArray().toString(Charsets.UTF_8)
        assertEquals(result, "")
        assertFalse(debridFile.exists())

    }
}