package io.william.debridav.test.integrationtest.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.william.debridav.debrid.premiumize.DirectDownloadResponseJackson
import org.mockserver.client.MockServerClient
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
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
                                "/premiumize/cache/check"
                        )
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("""
                            {
                                "response":[true],
                                "status": "ok",
                                "transcoded":["true"],
                                "filename":["filename"],
                                "filesize":[100]
                            }
                            """
                            )
        )
    }

    fun mockIsNotCached() {
        MockServerClient(
                "localhost", port
        ).`when`(
                HttpRequest.request()
                        .withMethod("GET")
                        .withPath(
                                "/premiumize/cache/check"
                        ), Times.exactly(1)
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody("{\"response\":[false]}")
        )
    }


    fun mockCachedContents() {
        val response = DirectDownloadResponseJackson(
                "success",
                "location",
                "filename",
                100,
                listOf(
                        DirectDownloadResponseJackson.Content(
                                "a/b/c/movie.mkv",
                                100000000,
                                "http://localhost:$port/workingLink",
                                null,
                                null
                        )
                )
        )
        MockServerClient(
                "localhost", port
        ).`when`(
                HttpRequest.request()
                        .withMethod("POST")
                        .withPath(
                                "/premiumize/transfer/directdl"
                        )
        ).respond(
                HttpResponse.response()
                        .withStatusCode(200)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(objectMapper.writeValueAsString(response))
        )
    }
}