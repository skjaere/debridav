package io.william.debrid

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.william.debrid.premiumize.DirectDownloadResponse
import org.mockserver.client.MockServerClient
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.mockserver.model.Parameter
import org.springframework.beans.factory.annotation.Value

class StubbingService(
    @Value("\${mockserver.port}") val port: Int
) {
    /*private var clientAndServer: ClientAndServer? = null

    init {
        clientAndServer = ClientAndServer.startClientAndServer(port)
    }

    companion object {
        private var instance: StubbingService? = null
        fun getInstance(): StubbingService {
            if (instance == null) {
                val port = TestSocketUtils.findAvailableTcpPort()

                instance = StubbingService(port)
            }
            return instance!!
        }
    }*/

    private val objectMapper = jacksonObjectMapper()

    fun mockIsCached() {
        MockServerClient(
            "localhost", port
        ).`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath(
                    "/cache/check"
                ), Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{\"response\":[true]}")
        )
    }

    fun mockIsNotCached() {
        MockServerClient(
            "localhost", port
        ).`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath(
                    "/cache/check"
                ), Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody("{\"response\":[false]}")
        )
    }

    fun mockDeadLink() {
        MockServerClient(
            "localhost", port
        ).`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath(
                    "deadLink"
                ), Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(404)
        )
    }

    fun mockWorkingStream() {
        MockServerClient(
            "localhost", port
        ).`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath(
                    "/workingLink"
                ), Times.exactly(2)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withBody("it works!")
        )
    }

    fun mockCachedContents() {
        val response = DirectDownloadResponse(
            "okay",
            "location",
            "filename",
            100,
            listOf(
                DirectDownloadResponse.Content(
                    "a/b/c",
                    100000000,
                    "http://localhost:$port/workingLink",
                    null,
                    "magnet"
                )
            )
        )
        MockServerClient(
            "localhost", port
        ).`when`(
            HttpRequest.request()
                .withMethod("POST")
                .withPath(
                    "/transfer/directdl"
                )
                .withQueryStringParameters(Parameter("src", "magnet")), Times.exactly(1)
        ).respond(
            HttpResponse.response()
                .withStatusCode(200)
                .withContentType(MediaType.APPLICATION_JSON)
                .withBody(objectMapper.writeValueAsString(response))
        )
    }
}