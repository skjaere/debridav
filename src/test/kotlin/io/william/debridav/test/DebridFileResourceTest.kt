package io.william.debridav.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.milton.http.Range
import io.william.debridav.StreamingService
import io.william.debridav.fs.DebridProvider
import io.william.debridav.fs.FileService
import io.william.debridav.resource.DebridFileResource
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.*
import java.io.ByteArrayOutputStream
import java.io.File

class DebridFileResourceTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun refreshesFileWhenDeadLinkIsReported() {
        //given
        val debridFile = File.createTempFile("/tmp", ".debridfile")
        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))

        val fileService: FileService = mock()
        val streamingService: StreamingService = mock()
        given(fileService.getDebridFileContents(debridFile)).willReturn(debridFileContents)
        val debridFileResource = DebridFileResource(debridFile, fileService, streamingService, DebridProvider.PREMIUMIZE)
        val outputStream = ByteArrayOutputStream()
        given(fileService.handleDeadLink(any())).willReturn(null)
        val content = debridFileContents.debridLinks.first()
        given(streamingService.streamDebridLink(
                eq(content),
                any(),
                anyLong(),
                anyOrNull()
        )).willReturn(StreamingService.Result.DEAD_LINK)

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

        val fileService: FileService = mock()
        val streamingService: StreamingService = mock()
        given(fileService.getDebridFileContents(debridFile)).willReturn(debridFileContents)
        val debridFileResource = DebridFileResource(debridFile, fileService, streamingService, DebridProvider.REAL_DEBRID)
        val outputStream = ByteArrayOutputStream()
        given(fileService.addProviderDebridLinkToDebridFile(debridFile)).willReturn(null)

        //when
        debridFileResource.sendContent(outputStream, Range(0, 1), mutableMapOf(), "video/mp4")

        //then
        verify(fileService, times(1)).addProviderDebridLinkToDebridFile(debridFile)
    }
}