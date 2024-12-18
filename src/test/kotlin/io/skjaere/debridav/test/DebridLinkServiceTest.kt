package io.skjaere.debridav.test

import io.ktor.utils.io.errors.IOException
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.skjaere.debridav.LinkCheckService
import io.skjaere.debridav.configuration.DebridavConfiguration
import io.skjaere.debridav.debrid.DebridLinkService
import io.skjaere.debridav.debrid.DebridTorrentService
import io.skjaere.debridav.debrid.client.model.NetworkErrorGetCachedFilesResponse
import io.skjaere.debridav.debrid.client.model.NotCachedGetCachedFilesResponse
import io.skjaere.debridav.debrid.client.model.ProviderErrorGetCachedFilesResponse
import io.skjaere.debridav.debrid.client.model.SuccessfulGetCachedFilesResponse
import io.skjaere.debridav.debrid.client.premiumize.PremiumizeClient
import io.skjaere.debridav.debrid.client.realdebrid.RealDebridClient
import io.skjaere.debridav.debrid.model.CachedFile
import io.skjaere.debridav.debrid.model.MissingFile
import io.skjaere.debridav.debrid.model.ProviderError
import io.skjaere.debridav.fs.DebridProvider
import io.skjaere.debridav.fs.DebridTorrentFileContents
import io.skjaere.debridav.fs.FileService
import io.skjaere.debridav.test.integrationtest.config.TestContextInitializer
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class DebridLinkServiceTest {
    private val premiumizeClient = mockk<PremiumizeClient>()
    private val clock = Clock.fixed(Instant.ofEpochMilli(1730477942L), ZoneId.systemDefault())
    private val realDebridClient = mockk<RealDebridClient>()
    private val linkCheckService = mockk<LinkCheckService>()
    private val debridTorrentService = mockk<DebridTorrentService>()
    private val debridClients = listOf(realDebridClient, premiumizeClient)
    private val debridavConfiguration = DebridavConfiguration(
        mountPath = "${TestContextInitializer.BASE_PATH}/debridav",
        debridClients = listOf(DebridProvider.REAL_DEBRID, DebridProvider.PREMIUMIZE),
        downloadPath = "${TestContextInitializer.BASE_PATH}/downloads",
        cacheLocalDebridFilesThresholdMb = 2,
        filePath = "${TestContextInitializer.BASE_PATH}/files",
        retriesOnProviderError = 3,
        waitAfterNetworkError = Duration.ofMillis(10000),
        delayBetweenRetries = Duration.ofMillis(1000),
        waitAfterMissing = Duration.ofMillis(1000),
        waitAfterProviderError = Duration.ofMillis(1000),
        readTimeoutMilliseconds = 1000,
        connectTimeoutMilliseconds = 1000,
        waitAfterClientError = Duration.ofMillis(1000),
        shouldDeleteNonWorkingFiles = true,
        torrentLifetime = Duration.ofMinutes(1),
        waitBeforeStartStream = Duration.ofMillis(1)
    )
    private var file: File? = null

    private val fileService = spyk(FileService(debridavConfiguration))

    private val underTest = DebridLinkService(
        debridavConfiguration = debridavConfiguration,
        debridClients = debridClients,
        fileService = fileService,
        linkCheckService = linkCheckService,
        debridTorrentService = debridTorrentService,
        clock = clock
    )

    @BeforeEach
    fun setup() {
        every { premiumizeClient.getProvider() } returns DebridProvider.PREMIUMIZE
        every { realDebridClient.getProvider() } returns DebridProvider.REAL_DEBRID
        every { fileService.getDebridFileContents(any()) } returns debridFileContents.deepCopy()
        if (file == null) {
            file = File.createTempFile("debridav", ".tmp")
        }
        file?.writeText("")
    }

    @Test
    fun thatGetCheckedLinksRespectsDebridProviderOrdering() {
        // given
        coEvery { linkCheckService.isLinkAlive(any()) } returns true

        // when
        val result = runBlocking {
            underTest.getCheckedLinks(file!!).firstOrNull()
        }

        assertEquals(result, realDebridCachedFile)
    }

    @Test
    fun thatCachedFileWithNonWorkingLinkGetsRefreshed() {
        // given
        coEvery { realDebridClient.isCached(eq(MAGNET)) } returns true
        coEvery { linkCheckService.isLinkAlive(eq(realDebridCachedFile.link)) } returns false
        coEvery {
            realDebridClient.getStreamableLink(
                debridFileContents.magnet,
                debridFileContents.debridLinks.first { it.provider == DebridProvider.REAL_DEBRID } as CachedFile)
        } returns "http://test.test/updated_bar.mkv"
        val freshCachedFile = CachedFile(
            path = "/foo/bar.mkv",
            provider = DebridProvider.REAL_DEBRID,
            size = 100L,
            link = "http://test.test/updated_bar.mkv",
            lastChecked = 100,
            params = mapOf(),
            mimeType = "video/mkv"
        )

        coEvery { realDebridClient.getCachedFiles(eq(debridFileContents.magnet)) } returns listOf(freshCachedFile)

        // when
        val result = runBlocking {
            underTest.getCheckedLinks(file!!).firstOrNull()
        }

        assertEquals(freshCachedFile, result)
    }

    @Test
    fun thatCachedFileWithNonWorkingLinkAndIsNotCachedGetsReplacedWithMissingLink() {
        // given
        mockIsNotCached()
        coEvery { linkCheckService.isLinkAlive(eq(realDebridCachedFile.link)) } returns false
        coEvery { linkCheckService.isLinkAlive(eq(premiumizeCachedFile.link)) } returns true
        //coEvery { realDebridClient.getCachedFiles(eq(debridFileContents.magnet)) } returns listOf()
        coEvery {
            realDebridClient.getStreamableLink(
                debridFileContents.magnet,
                debridFileContents.debridLinks.first { it.provider == DebridProvider.REAL_DEBRID } as CachedFile)
        } returns null
        coEvery {
            debridTorrentService.getCachedFiles(debridFileContents.magnet, listOf(realDebridClient))
        } returns flowOf(
            NotCachedGetCachedFilesResponse(DebridProvider.REAL_DEBRID),
        )
        coEvery {
            debridTorrentService.getCachedFiles(debridFileContents.magnet, listOf(premiumizeClient))
        } returns flowOf(
            SuccessfulGetCachedFilesResponse(listOf(premiumizeCachedFile), DebridProvider.PREMIUMIZE)
        )

        // when
        val result = runBlocking {
            underTest.getCheckedLinks(file!!).firstOrNull()
        }
        val updatedFileContents = Json.decodeFromString<DebridTorrentFileContents>(file!!.readText())

        // then
        assertEquals(result, premiumizeCachedFile)
        assertEquals(
            DebridTorrentFileContents(
                debridFileContents.originalPath,
                debridFileContents.size,
                debridFileContents.modified,
                debridFileContents.magnet,
                mutableListOf(
                    MissingFile(DebridProvider.REAL_DEBRID, Instant.now(clock).toEpochMilli()),
                    premiumizeCachedFile
                )
            ),
            updatedFileContents
        )
    }

    @Test
    fun thatCachedFileWithNonWorkingLinkAndIsCachedGetsReplacedWithProviderErrorOnError() {
        // given
        mockIsCached()
        coEvery { linkCheckService.isLinkAlive(eq(realDebridCachedFile.link)) } returns false
        coEvery { linkCheckService.isLinkAlive(eq(premiumizeCachedFile.link)) } returns true
        //coEvery { realDebridClient.getCachedFiles(eq(debridFileContents.magnet)) } throws mock<DebridProviderError>()
        coEvery {
            realDebridClient.getStreamableLink(
                debridFileContents.magnet,
                debridFileContents.debridLinks.first { it.provider == DebridProvider.REAL_DEBRID } as CachedFile)
        } returns null
        coEvery {
            debridTorrentService.getCachedFiles(debridFileContents.magnet, listOf(realDebridClient))
        } returns flowOf(
            ProviderErrorGetCachedFilesResponse(DebridProvider.REAL_DEBRID),
        )
        coEvery {
            debridTorrentService.getCachedFiles(debridFileContents.magnet, listOf(premiumizeClient))
        } returns flowOf(
            SuccessfulGetCachedFilesResponse(listOf(premiumizeCachedFile), DebridProvider.PREMIUMIZE)
        )
        // when
        val result = runBlocking {
            underTest.getCheckedLinks(file!!).firstOrNull()
        }
        val updatedFileContents = Json.decodeFromString<DebridTorrentFileContents>(file!!.readText())

        // then
        assertEquals(premiumizeCachedFile, result)
        assertEquals(
            DebridTorrentFileContents(
                debridFileContents.originalPath,
                debridFileContents.size,
                debridFileContents.modified,
                debridFileContents.magnet,
                mutableListOf(
                    ProviderError(DebridProvider.REAL_DEBRID, Instant.now(clock).toEpochMilli()),
                    premiumizeCachedFile
                )
            ),
            updatedFileContents
        )
    }

    @Test
    fun thatCachedFileWithNonWorkingLinkAndIsCachedDoesNotGetReplacedOnNetworkError() {
        // given
        mockIsCached()
        coEvery { linkCheckService.isLinkAlive(eq(realDebridCachedFile.link)) } returns false
        coEvery { linkCheckService.isLinkAlive(eq(premiumizeCachedFile.link)) } returns true
        coEvery {
            realDebridClient.getStreamableLink(
                debridFileContents.magnet,
                debridFileContents.debridLinks.first { it.provider == DebridProvider.REAL_DEBRID } as CachedFile)
        } returns null
        coEvery {
            debridTorrentService.getCachedFiles(debridFileContents.magnet, listOf(realDebridClient))
        } returns flowOf(
            NetworkErrorGetCachedFilesResponse(DebridProvider.REAL_DEBRID),
        )
        coEvery {
            debridTorrentService.getCachedFiles(debridFileContents.magnet, listOf(premiumizeClient))
        } returns flowOf(
            SuccessfulGetCachedFilesResponse(listOf(premiumizeCachedFile), DebridProvider.PREMIUMIZE)
        )

        coEvery { realDebridClient.getCachedFiles(eq(debridFileContents.magnet)) } throws IOException()

        // when
        val result = runBlocking {
            underTest.getCheckedLinks(file!!).firstOrNull()
        }
        val updatedFileContents = Json.decodeFromString<DebridTorrentFileContents>(file!!.readText())

        // then
        assertEquals(premiumizeCachedFile, result)
        assertEquals(debridFileContents, updatedFileContents)
    }

    @Test
    fun thatDebridLinkGetsAddedToDebridFileContentsWhenProviderIsMissing() {
        // given
        coEvery { realDebridClient.getCachedFiles(eq(MAGNET)) } returns listOf(realDebridCachedFile)
        coEvery { realDebridClient.isCached(eq(MAGNET)) } returns true
        coEvery { linkCheckService.isLinkAlive(any()) } returns true

        val debridFileContentsWithoutRealDebridLink = debridFileContents.deepCopy()
        debridFileContentsWithoutRealDebridLink.debridLinks.removeFirst()

        // when
        val result = runBlocking { underTest.getCheckedLinks(file!!).first() }

        // then
        assertEquals(result.provider, DebridProvider.REAL_DEBRID)
    }

    @Test
    fun thatMissingLinkGetsReplacedWithCachedLinkWhenProviderHasFile() {
        // given
        coEvery { realDebridClient.getCachedFiles(eq(MAGNET)) } returns listOf(realDebridCachedFile)
        coEvery { realDebridClient.isCached(eq(MAGNET)) } returns true
        coEvery { linkCheckService.isLinkAlive(eq(realDebridCachedFile.link)) } returns true

        val debridFileContentsWithMissingRealDebridLink = debridFileContents.deepCopy()
        debridFileContentsWithMissingRealDebridLink.debridLinks = mutableListOf(
            MissingFile(DebridProvider.REAL_DEBRID, Instant.now(clock).minus(25, ChronoUnit.HOURS).toEpochMilli()),
            debridFileContents.debridLinks.last()
        )
        every { fileService.getDebridFileContents(any()) } returns debridFileContents.deepCopy()

        // when
        val result = runBlocking { underTest.getCheckedLinks(file!!).first() }

        // then
        assertEquals(result.provider, DebridProvider.REAL_DEBRID)
    }

    @Test
    fun thatRecentlyCheckedDebridFileDoesNotGetReChecked() {
        // given
        mockIsCached()
        val debridFileContentsWithMissingRealDebridLink = debridFileContents.deepCopy()
        debridFileContentsWithMissingRealDebridLink.debridLinks = mutableListOf(
            MissingFile(DebridProvider.REAL_DEBRID, Instant.now(clock).minus(1, ChronoUnit.HOURS).toEpochMilli()),
            debridFileContents.debridLinks.last()
        )
        every { fileService.getDebridFileContents(any()) } returns debridFileContentsWithMissingRealDebridLink.deepCopy()
        coEvery {
            realDebridClient.getStreamableLink(
                realDebridCachedFile.link,
                realDebridCachedFile
            )
        } returns realDebridCachedFile.link
        coEvery { linkCheckService.isLinkAlive(eq(realDebridCachedFile.link)) } returns true
        coEvery { linkCheckService.isLinkAlive(eq(premiumizeCachedFile.link)) } returns true

        coEvery {
            debridTorrentService.getCachedFiles(debridFileContents.magnet, listOf(realDebridClient))
        } returns flowOf(
            NotCachedGetCachedFilesResponse(DebridProvider.REAL_DEBRID),
        )
        coEvery {
            debridTorrentService.getCachedFiles(debridFileContents.magnet, listOf(premiumizeClient))
        } returns flowOf(
            SuccessfulGetCachedFilesResponse(listOf(premiumizeCachedFile), DebridProvider.PREMIUMIZE)
        )
        // when
        val result = runBlocking { underTest.getCheckedLinks(file!!).first() }

        // then
        assertEquals(DebridProvider.PREMIUMIZE, result.provider)
    }

    private fun mockIsNotCached() {
        coEvery { realDebridClient.isCached(eq(MAGNET)) } returns false
        coEvery { premiumizeClient.isCached(eq(MAGNET)) } returns false
    }

    private fun mockIsCached() {
        coEvery { realDebridClient.isCached(eq(MAGNET)) } returns true
        coEvery { premiumizeClient.isCached(eq(MAGNET)) } returns true
    }


    private fun DebridTorrentFileContents.deepCopy() =
        Json.decodeFromString<DebridTorrentFileContents>(
            Json.encodeToString(DebridTorrentFileContents.serializer(), this)
        )
}
