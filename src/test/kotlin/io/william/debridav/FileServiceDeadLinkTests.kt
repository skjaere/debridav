package io.william.debridav

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.milton.http.Range
import io.william.debridav.debrid.DebridLink
import io.william.debridav.debrid.premiumize.PremiumizeClient
import io.william.debridav.fs.FileService
import io.william.debridav.fs.models.DebridFileContents
import io.william.debridav.resource.DebridFileResource
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

        given(premiumizeClient.isCached("magnet")).willReturn(true)

        val directDownloadResponse = listOf(DebridLink(
                "a/b/c",
                1000,
                "video/mp4",
                "https://test.com/video.mkv",
        ))

        given(premiumizeClient.getDirectDownloadLink("magnet"))
                .willReturn(directDownloadResponse)

        doAnswer {
            outputStream.write("It works!".toByteArray())
            StreamingService.Result.OK
        }.`when`(
                streamingService
        ).streamDebridFile(
                argThat(
                        DebridContentsMatcher(
                                DebridFileContents.ofDebridLink(
                                        directDownloadResponse.first(),
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