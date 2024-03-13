package io.william.debrid

import com.fasterxml.jackson.databind.ObjectMapper
import io.william.debrid.fs.FileService
import io.william.debrid.fs.models.DebridFileContents
import io.william.debrid.premiumize.DirectDownloadResponse
import io.william.debrid.premiumize.PremiumizeClient
import io.william.debrid.resource.DebridFileResource
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockserver.client.MockServerClient
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.matchers.Times.exactly
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response
import org.mockserver.model.MediaType
import org.mockserver.model.Parameter
import java.io.ByteArrayOutputStream
import java.io.File


class FileServiceDeadLinkTests {
    private val objectMapper = ObjectMapper()


    companion object {
        private var mockServer: ClientAndServer? = null

        @JvmStatic
        @BeforeAll
        fun startServer() {
            mockServer = startClientAndServer(8099)
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

        mockDeadLink()
        mockIsNotCached()

        val premiumizeClient = PremiumizeClient(
            "abd", "http://localhost:8099"
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
            "http://localhost:8099/deadLink",
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
        assertEquals(result,null)
        assertFalse(debridFile.exists())
    }

    @Test
    fun deletesFileWhenLinkCannotBeRefreshed() {
        //given
        mockIsCached()
        mockDeadLink()
        mockFreshContents()
        mockWorkingStream()

        val premiumizeClient = PremiumizeClient(
            "abd", "http://localhost:8099"
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
            "http://localhost:8099/deadLink",
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
        assertEquals(result,"it works!")
    }

    private fun mockIsCached() {
        MockServerClient(
            "localhost",8099
        ).`when`(
            request()
                .withMethod("GET")
                .withPath(
                    "/cache/check"
                ), exactly(1)
        ).respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{\"response\":[true]}")
        )
    }

    private fun mockIsNotCached() {
        MockServerClient(
            "localhost",8099
        ).`when`(
            request()
                .withMethod("GET")
                .withPath(
                    "/cache/check"
                ), exactly(1)
        ).respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{\"response\":[false]}")
        )
    }

    private fun mockDeadLink() {
        MockServerClient(
            "localhost", 8099
        ).`when`(
            request()
                .withMethod("GET")
                .withPath(
                    "deadLink"
                ), exactly(1)
        ).respond(
            response()
                .withStatusCode(404)
        )
    }

    private fun mockWorkingStream() {
        MockServerClient(
            "localhost",8099
        ).`when`(
            request()
                .withMethod("GET")
                .withPath(
                    "/workingLink"
                ), exactly(2)
        ).respond(
            response()
                .withStatusCode(200)
                .withBody("it works!")
        )
    }

    private fun mockFreshContents() {
        val response = DirectDownloadResponse(
            "okay",
            "location",
            "filename",
            100,
            listOf(
                DirectDownloadResponse.Content(
                    "a/b/c",
                    100,
                    "http://localhost:8099/workingLink",
                    null,
                    "magnet"
                )
            )
        )
        MockServerClient(
            "localhost",8099
        ).`when`(
            request()
                .withMethod("POST")
                .withPath(
                    "/transfer/directdl"
                )
                .withQueryStringParameters(Parameter("src", "magnet"))
            , exactly(1)
        ).respond(
            response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(objectMapper.writeValueAsString(response))
        )
    }
}