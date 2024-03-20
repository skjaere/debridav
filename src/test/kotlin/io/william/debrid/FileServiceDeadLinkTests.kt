package io.william.debrid

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.milton.http.Range
import io.william.debrid.fs.FileService
import io.william.debrid.fs.models.DebridFileContents
import io.william.debrid.premiumize.DirectDownloadResponse
import io.william.debrid.premiumize.PremiumizeClient
import io.william.debrid.resource.DebridFileResource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import java.io.ByteArrayOutputStream
import java.io.File


class FileServiceDeadLinkTests {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun servesRefreshedStreamWhenEncountering404() {
        val premiumizeClient = mock(PremiumizeClient::class.java)
        val streamingService = mock(StreamingService::class.java)
        val fileService = FileService(
            premiumizeClient,
            "/tmp/debridtest/files",
            2,
            streamingService
        )
        val debridFile = File.createTempFile("/tmp", ".debridfile")
        val debridFileContents = DebridFileContents(
            "a/b/c",
            100,
            1000,
            "http://localhost:999/deadLink",
            "magnet"
        )

        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileResource = DebridFileResource(
            debridFile, fileService, streamingService
        )
        val outputStream = ByteArrayOutputStream()
        given(
            streamingService.streamDebridFile(
                eq(debridFileContents),
                any(),
                any()
            )
        ).willReturn(StreamingService.Result.DEAD_LINK)

        given(
            premiumizeClient.isCached("magnet")
        ).willReturn(true)
        val directDownloadResponse = DirectDownloadResponse(
            "ok",
            "a/b",
            "https://test.com/video.mkv",
            10000,
            listOf(
                DirectDownloadResponse.Content(
                    "a/b/c",
                    1000,
                    "https://test.com/video.mkv",
                    null,
                    "not_needed"
                )
            )
        )
        given(
            premiumizeClient.getDirectDownloadLink("magnet")
        ).willReturn(directDownloadResponse)

        doAnswer {
            outputStream.write("It works!".toByteArray())
            StreamingService.Result.OK
        }.`when`(
            streamingService
        ).streamDebridFile(
            argThat(
                DebridContentsMatcher(
                    DebridFileContents.ofDebridResponseContents(
                        directDownloadResponse.content.first(),
                        "magnet"
                    )
                )
            ),
            any(),
            any()
        )


        //when
        fileResource.sendContent(
            outputStream, Range(0, 1), null, null
        )

        //then
        val result = outputStream.toByteArray().toString(Charsets.UTF_8)
        assertEquals(result, "It works!")
    }

    @Test
    fun deletesFileWhenLinkCannotBeRefreshed() {
        //given
        val premiumizeClient = mock(PremiumizeClient::class.java)
        val streamingService = mock(StreamingService::class.java)
        val fileService = FileService(
            premiumizeClient,
            "/tmp/debridtest/files",
            2,
            streamingService
        )
        val debridFile = File.createTempFile("/tmp", ".debridfile")
        val debridFileContents = DebridFileContents(
            "a/b/c",
            100,
            1000,
            "https://test.com/deadLink",
            "magnet"
        )
        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileResource = DebridFileResource(
            debridFile, fileService, streamingService
        )

        given(streamingService.streamDebridFile(any(), any(), any())).willReturn(StreamingService.Result.DEAD_LINK)
        given(premiumizeClient.isCached(eq("magnet"))).willReturn(false)

        //when
        val outputStream = ByteArrayOutputStream()
        fileResource.sendContent(
            outputStream, Range(0, 1), null, null
        )

        //then
        val result = outputStream.toByteArray().toString(Charsets.UTF_8)
        assertEquals(result, "")
        assertFalse(debridFile.exists())
    }
}