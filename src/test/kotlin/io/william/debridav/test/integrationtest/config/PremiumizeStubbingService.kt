package io.william.debridav.test.integrationtest.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.william.debridav.debrid.premiumize.DirectDownloadResponse
import org.mockserver.client.MockServerClient
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import org.mockserver.model.Parameter
import org.springframework.beans.factory.annotation.Value

class PremiumizeStubbingService(
        @Value("\${mockserver.port}") val port: Int
) {
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


    fun mockCachedContents() {
        val response = DirectDownloadResponse(
                "okay",
                "location",
                "filename",
                100,
                listOf(
                        DirectDownloadResponse.Content(
                                "a/b/c/movie.mkv",
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