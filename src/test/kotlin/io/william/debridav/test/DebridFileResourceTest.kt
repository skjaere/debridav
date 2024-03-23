package io.william.debridav.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.milton.http.Range
import io.william.debridav.StreamingService
import io.william.debridav.fs.FileService
import io.william.debridav.resource.DebridFileResource
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import java.io.ByteArrayOutputStream
import java.io.File

class DebridFileResourceTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun refreshesFileWhenDeadLinkIsReported() {
        //given
        val debridFile = File.createTempFile("/tmp", ".debridfile")
        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))

        val fileService = mock(FileService::class.java)
        val streamingService = mock(StreamingService::class.java)
        val debridFileResource = DebridFileResource(debridFile, fileService, streamingService)
        val outputStream = ByteArrayOutputStream()
        given(streamingService.streamDebridFile(eq(debridFileContents), any(), any()))
                .willReturn(StreamingService.Result.DEAD_LINK)
        given(fileService.handleDeadLink(debridFile)).willReturn(null)

        //when
        debridFileResource.sendContent(outputStream, Range(0, 1), mutableMapOf(), "video/mp4")

        //then
        verify(fileService, times(1)).handleDeadLink(debridFile)
    }

    @Test
    fun addsProviderToDebridFileWhenProviderIsMissing() {
        //given
        val debridFile = File.createTempFile("/tmp", ".debridfile")
        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))

        val fileService = mock(FileService::class.java)
        val streamingService = mock(StreamingService::class.java)
        val debridFileResource = DebridFileResource(debridFile, fileService, streamingService)
        val outputStream = ByteArrayOutputStream()
        given(streamingService.streamDebridFile(eq(debridFileContents), any(), any()))
                .willReturn(StreamingService.Result.PROVIDER_MISSING)
        given(fileService.addProviderDebridLinkToDebridFile(debridFile)).willReturn(null)

        //when
        debridFileResource.sendContent(outputStream, Range(0, 1), mutableMapOf(), "video/mp4")

        //then
        verify(fileService, times(1)).addProviderDebridLinkToDebridFile(debridFile)
    }
}