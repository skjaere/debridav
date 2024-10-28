package io.william.debridav.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.william.debridav.StreamingService
import io.william.debridav.configuration.DebridavConfiguration
import io.william.debridav.debrid.premiumize.PremiumizeClient
import io.william.debridav.debrid.realdebrid.RealDebridClient
import io.william.debridav.fs.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.io.File


class FileServiceTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun updatesFileAndReturnsRefreshedContentWhenEncountering404() {
        //given
        val premiumizeClient = mock(PremiumizeClient::class.java)
        given(premiumizeClient.getProvider()).willReturn(DebridProvider.PREMIUMIZE)
        val streamingService = mock(StreamingService::class.java)
        val fileContentsService = mock(FileContentsService::class.java)
        val debridFile = File.createTempFile("/tmp", ".debridfile")
        val debridavConfiguration = DebridavConfiguration(
            "/tmp/debridtest/files",
            "/downloads",
            "",
            2
        )

        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileService = FileService(
            premiumizeClient,
            debridavConfiguration,
            streamingService,
            fileContentsService
        )

        given(premiumizeClient.isCached(magnet))
            .willReturn(true)
        given(premiumizeClient.getDirectDownloadLink(magnet))
            .willReturn(directDownloadResponse)

        //when
        val result = fileService.handleDeadLink(debridFile)

        //then
        assertEquals(
            directDownloadResponse.first().link,
            result?.debridLinks?.first()?.link
        )
        assertEquals(
            directDownloadResponse.first().link,
            jacksonObjectMapper().readValue<DebridFileContents>(debridFile.readText()).debridLinks.first().link
        )
    }

    @Test
    fun updatesFileAndReturnsRefreshedContentWhenEncountering404AndFileHasMultipleProviders() {
        //given
        val premiumizeClient = mock(PremiumizeClient::class.java)
        given(premiumizeClient.getProvider()).willReturn(DebridProvider.PREMIUMIZE)
        val streamingService = mock(StreamingService::class.java)
        val debridFile = File.createTempFile("/tmp", ".debridfile")
        val contents = debridFileContents.copy()
        val fileContentsService = mock(FileContentsService::class.java)
        val debridavConfiguration = DebridavConfiguration(
            "/tmp/debridtest/files",
            "/downloads",
            "",
            2
        );
        contents.debridLinks.add(DebridLink(DebridProvider.REAL_DEBRID, "http://localhost/deadLink"))

        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileService = FileService(
            premiumizeClient,
            debridavConfiguration,
            streamingService,
            fileContentsService
        )

        given(premiumizeClient.isCached(magnet))
            .willReturn(true)
        given(premiumizeClient.getDirectDownloadLink(magnet))
            .willReturn(directDownloadResponse)

        //when
        val result = fileService.handleDeadLink(debridFile)

        //then
        assertEquals(
            directDownloadResponse.first().link,
            result?.debridLinks?.first()?.link
        )
        assertEquals(
            directDownloadResponse.first().link,
            jacksonObjectMapper().readValue<DebridFileContents>(debridFile.readText()).debridLinks.first().link
        )
    }

    @Test
    fun deletesFileWhenLinkCannotBeRefreshed() {
        //given
        val premiumizeClient = mock(PremiumizeClient::class.java)
        given(premiumizeClient.getProvider()).willReturn(DebridProvider.PREMIUMIZE)
        val streamingService = mock(StreamingService::class.java)
        val debridFile = File.createTempFile("/tmp", ".debridfile")
        val fileContentsService = mock(FileContentsService::class.java)
        val debridavConfiguration = DebridavConfiguration(
            "/tmp/debridtest/files",
            "/downloads",
            "",
            2
        );

        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileService = FileService(
            premiumizeClient,
            debridavConfiguration,
            streamingService,
            fileContentsService
        )
        given(premiumizeClient.isCached(magnet))
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
        given(debridClient.getProvider()).willReturn(DebridProvider.REAL_DEBRID)
        val streamingService = mock(StreamingService::class.java)
        val debridFile = File.createTempFile("/tmp", ".debridfile")
        val fileContentsService = mock(FileContentsService::class.java)
        val debridavConfiguration = DebridavConfiguration(
            "/tmp/debridtest/files",
            "/downloads",
            "",
            2
        );

        debridFile.writeText(objectMapper.writeValueAsString(debridFileContents))
        val fileService = FileService(
            debridClient,
            debridavConfiguration,
            streamingService,
            fileContentsService
        )
        given(debridClient.getProvider())
            .willReturn(DebridProvider.REAL_DEBRID)
        given(debridClient.isCached(magnet))
            .willReturn(true)
        given(debridClient.getDirectDownloadLink(magnet))
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