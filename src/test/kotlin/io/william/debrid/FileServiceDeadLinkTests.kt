package io.william.debrid

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.william.debrid.fs.FileService
import io.william.debrid.fs.models.DebridFileContents
import io.william.debrid.premiumize.PremiumizeClient
import io.william.debrid.resource.DebridFileResource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.springframework.test.util.TestSocketUtils
import java.io.ByteArrayOutputStream
import java.io.File


class FileServiceDeadLinkTests {
    private val objectMapper = jacksonObjectMapper()

    companion object {
        private var mockServer: ClientAndServer? = null
        val port = TestSocketUtils.findAvailableTcpPort()

        @JvmStatic
        @BeforeAll
        fun startServer() {
            mockServer = startClientAndServer(port)
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            mockServer!!.stop()
        }
    }

    @Test
    fun servesRefreshedStreamWhenEncountering404() {
        //given

        mockDeadLink(port)
        mockIsCached(port)
        mockCachedContents(port)
        mockWorkingStream(port)


        val premiumizeClient = PremiumizeClient(
            "abd", "http://localhost:$port"
        )
        val fileService = FileService(
            premiumizeClient,
            "/tmp/debridtest/files",
            2
        )
        val debridFile = File("/tmp/debridtest")
        val debridFileContents = DebridFileContents(
            "a/b/c",
            100,
            1000,
            "http://localhost:$port/deadLink",
            "magnet"
        )
        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileResource = DebridFileResource(
            debridFile, fileService
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
        mockIsNotCached(port)
        mockDeadLink(port)


        val premiumizeClient = PremiumizeClient(
            "abd", "http://localhost:$port"
        )
        val fileService = FileService(
            premiumizeClient,
            "/tmp/debridtest/files",
            2
        )
        val debridFile = File("/tmp/debridtest")
        val debridFileContents = DebridFileContents(
            "a/b/c",
            100,
            1000,
            "http://localhost:$port/deadLink",
            "magnet"
            //null
        )
        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileResource = DebridFileResource(
            debridFile, fileService
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