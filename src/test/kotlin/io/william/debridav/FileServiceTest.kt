package io.william.debridav

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.william.debridav.debrid.premiumize.PremiumizeClient
import io.william.debridav.debrid.realdebrid.RealDebridClient
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.fs.DebridProvider
import io.william.debridav.fs.FileService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.io.File


class FileServiceTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun servesRefreshedStreamWhenEncountering404() {
        //given
        val premiumizeClient = mock(PremiumizeClient::class.java)
        val streamingService = mock(StreamingService::class.java)
        val debridFile = File.createTempFile("/tmp", ".debridfile")

        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileService = FileService(
                premiumizeClient,
                "/tmp/debridtest/files",
                "/downloads",
                2,
                DebridProvider.PREMIUMIZE,
                streamingService
        )

        given(premiumizeClient.isCached("magnet"))
                .willReturn(true)
        given(premiumizeClient.getDirectDownloadLink("magnet"))
                .willReturn(directDownloadResponse)

        //when
        val result = fileService.handleDeadLink(debridFile)

        //then
        assertEquals(
                DebridFileContents.ofDebridResponse(directDownloadResponse.first(), "magnet", DebridProvider.PREMIUMIZE),
                result
        )
        assertEquals(DebridFileContents.ofDebridResponse(directDownloadResponse.first(), "magnet", DebridProvider.PREMIUMIZE),
                jacksonObjectMapper().readValue<DebridFileContents>(debridFile.readText())
        )
    }

    @Test
    fun deletesFileWhenLinkCannotBeRefreshed() {
        //given
        val premiumizeClient = mock(PremiumizeClient::class.java)
        val streamingService = mock(StreamingService::class.java)
        val debridFile = File.createTempFile("/tmp", ".debridfile")

        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileService = FileService(
                premiumizeClient,
                "/tmp/debridtest/files",
                "/downloads",
                2,
                DebridProvider.PREMIUMIZE,
                streamingService
        )
        given(premiumizeClient.isCached("magnet"))
                .willReturn(false)

        //when
        val result = fileService.handleDeadLink(debridFile)

        //then
        assertEquals(null, result)
        assertFalse(debridFile.exists())
    }

    @Test
    fun addsDebridLinkToFileWhenProviderIsMissing() {
        //given
        val debridClient = mock(RealDebridClient::class.java)
        val streamingService = mock(StreamingService::class.java)
        val debridFile = File.createTempFile("/tmp", ".debridfile")

        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileService = FileService(
                debridClient,
                "/tmp/debridtest/files",
                "/downloads",
                2,
                DebridProvider.PREMIUMIZE,
                streamingService
        )
        given(debridClient.getProvider())
                .willReturn(DebridProvider.REAL_DEBRID)
        given(debridClient.isCached("magnet"))
                .willReturn(true)
        given(debridClient.getDirectDownloadLink("magnet"))
                .willReturn(directDownloadResponse)

        //when
        val result = fileService.addProviderDebridLinkToDebridFile(debridFile)

        //then
        val expectedContents = debridFileContents.copy()
        expectedContents.debridLinks.add(directDownloadResponse.first().toDebridLink(DebridProvider.REAL_DEBRID))

        assertEquals(
                expectedContents,
                result
        )
        assertEquals(
                expectedContents,
                jacksonObjectMapper().readValue<DebridFileContents>(debridFile.readText())
        )
    }
}