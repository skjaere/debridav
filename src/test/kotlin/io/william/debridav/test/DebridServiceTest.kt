package io.william.debridav.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.spyk
import io.william.debridav.LinkCheckService
import io.william.debridav.configuration.DebridavConfiguration
import io.william.debridav.debrid.CachedFile
import io.william.debridav.debrid.DebridFileContentsDeserializer
import io.william.debridav.debrid.DebridService
import io.william.debridav.debrid.MissingFile
import io.william.debridav.debrid.premiumize.PremiumizeClient
import io.william.debridav.debrid.realdebrid.RealDebridClient
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.fs.DebridProvider
import io.william.debridav.fs.FileService
import io.william.debridav.test.integrationtest.config.TestContextInitializer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals


class DebridServiceTest {
    private val premiumizeClient = mock<PremiumizeClient>()
    private val clock = Clock.fixed(Instant.ofEpochMilli(1730477942L), ZoneId.systemDefault())
    private val realDebridClient = mock<RealDebridClient>()
    private val linkCheckService = mock<LinkCheckService>()
    private val debridClients = listOf(realDebridClient, premiumizeClient)
    private val debridavConfiguration = DebridavConfiguration(
        mountPath = "${TestContextInitializer.BASE_PATH}/debridav",
        debridClients = listOf(DebridProvider.REAL_DEBRID, DebridProvider.PREMIUMIZE),
        downloadPath = "${TestContextInitializer.BASE_PATH}/downloads",
        cacheLocalDebridFilesThresholdMb = 2,
        filePath = "${TestContextInitializer.BASE_PATH}/files"
    )
    private var file: File? = null

    private val fileService = spyk(FileService(debridavConfiguration))

    private val underTest = DebridService(
        debridavConfiguration = debridavConfiguration,
        debridClients = debridClients,
        fileService = fileService,
        linkCheckService = linkCheckService,
        clock = clock
    )
    @BeforeEach
    fun setup() {
        given(premiumizeClient.getProvider()).willReturn(DebridProvider.PREMIUMIZE)
        given(realDebridClient.getProvider()).willReturn(DebridProvider.REAL_DEBRID)
        every { fileService.getDebridFileContents(any()) } returns debridFileContents.deepCopy()
        if(file == null) {
            file = File.createTempFile("debridav", ".tmp")
        }
        file?.writeText("")
    }

    @AfterEach
    fun teardown() {
        file?.delete()
    }

    @Test
    fun thatIsCachedReturnsTrueWhenOneDebridClientReturnsTrue() {
        //given
        premiumizeClient.stub {
            onBlocking { isCached(eq(magnet)) }.thenReturn(false)
        }
        realDebridClient.stub {
            onBlocking { isCached(eq(magnet)) }.thenReturn(true)
        }

        //when
        val result = underTest.isCached(magnet)

        //then
        assertTrue(result)
    }

    @Test
    fun thatIsCachedReturnsFalseWhenTwoDebridClientsReturnsFalse() {
        //given
        premiumizeClient.stub {
            onBlocking { isCached(eq(magnet)) }.thenReturn(false)
        }
        realDebridClient.stub {
            onBlocking { isCached(any()) }.thenReturn(true)
        }

        //when
        val result = underTest.isCached(magnet)

        //then
        assertTrue(result)
    }

    @Test
    fun thatGettingDebridFileContentsByMagnetWorks() {
        //given
        premiumizeClient.stub {
            onBlocking { isCached(eq(magnet)) }.thenReturn(true)
        }
        premiumizeClient.stub {
            onBlocking { getCachedFiles(eq(magnet)) }.thenReturn(listOf(premiumizeCachedFile))
        }
        realDebridClient.stub {
            onBlocking { getCachedFiles(eq(magnet)) }.thenReturn(listOf(realDebridCachedFile))
        }
        realDebridClient.stub {
            onBlocking { isCached(any()) }.thenReturn(true)
        }

        //when
        val result = runBlocking { underTest.getDebridFiles(magnet) }
        assertEquals(debridFileContents, result.first())
    }

    @Test
    fun thatGettingDebridFileContentsByMagnetWorksWhenMissingFromPremiumize() {
        //given
        premiumizeClient.stub {
            onBlocking { isCached(eq(magnet)) }.thenReturn(false)
        }
        premiumizeClient.stub {
            onBlocking { getCachedFiles(eq(magnet)) }.thenReturn(listOf())
        }
        realDebridClient.stub {
            onBlocking { getCachedFiles(eq(magnet)) }.thenReturn(listOf(realDebridCachedFile))
        }
        realDebridClient.stub {
            onBlocking { isCached(any()) }.thenReturn(true)
        }

        //when
        val result = runBlocking { underTest.getDebridFiles(magnet) }

        //then
        val expectedDebridFileContents = debridFileContents.deepCopy()
        expectedDebridFileContents.debridLinks = mutableListOf(
            debridFileContents.debridLinks.first(),
            MissingFile(DebridProvider.PREMIUMIZE, Instant.now(clock).toEpochMilli())
        )
        assertEquals(expectedDebridFileContents, result.first())
    }

    @Test
    fun thatGetCheckedLinksRespectsDebridProviderOrdering() {
        //given
        given(linkCheckService.isLinkAlive(any())).willReturn(true)

        //when
        val result = runBlocking {
            underTest.getCheckedLinks(file!!).firstOrNull()
        }

        assertEquals(result, realDebridCachedFile)
    }

    @Test
    fun thatCachedFileWithNonWorkingLinkGetsRefreshed() {
        //given
        realDebridClient.stub {
            onBlocking { isCached(eq(magnet)) }.thenReturn(true)
        }
        given(linkCheckService.isLinkAlive(eq(realDebridCachedFile))).willReturn(false)
        given(linkCheckService.isLinkAlive(eq(realDebridCachedFile))).willReturn(false)
        val freshCachedFile = CachedFile(
            path = "/foo/bar.mkv",
            provider = DebridProvider.REAL_DEBRID,
            size = 100L,
            link = "http://test.test/updated_bar.mkv",
            lastChecked = 100,
            params = mapOf(),
            mimeType = "video/mkv"
        )


        realDebridClient.stub {
            onBlocking {
                getCachedFiles(eq(debridFileContents.magnet))
            }.thenReturn(
                listOf(freshCachedFile)
            )
        }

        //when
        val result = runBlocking {
            underTest.getCheckedLinks(file!!).firstOrNull()
        }

        assertEquals(freshCachedFile, result)
    }

    @Test
    fun thatCachedFileWithNonWorkingLinkAndIsNotCachedGetsReplacedWithMissingLink() {
        //given
        mockIsNotCached()
        given(linkCheckService.isLinkAlive(eq(realDebridCachedFile))).willReturn(false)
        given(linkCheckService.isLinkAlive(eq(realDebridCachedFile))).willReturn(false)
        given(linkCheckService.isLinkAlive(eq(premiumizeCachedFile))).willReturn(true)

        realDebridClient.stub {
            onBlocking {
                getCachedFiles(eq(debridFileContents.magnet))
            }.thenReturn(
                listOf()
            )
        }

        //when
        val result = runBlocking {
            underTest.getCheckedLinks(file!!).firstOrNull()
        }
        val updatedFileContents = DebridFileContentsDeserializer.deserialize(file!!.readText())

        //then
        assertEquals(result, premiumizeCachedFile)
        assertEquals(DebridFileContents(
            debridFileContents.originalPath,
            debridFileContents.size,
            debridFileContents.modified,
            debridFileContents.magnet,
            mutableListOf(MissingFile(DebridProvider.REAL_DEBRID, Instant.now(clock).toEpochMilli()),premiumizeCachedFile)
        ), updatedFileContents)
    }

    private fun mockIsNotCached() {
        realDebridClient.stub {
            onBlocking { isCached(eq(magnet)) }.thenReturn(false)
        }
        premiumizeClient.stub {
            onBlocking { isCached(eq(magnet)) }.thenReturn(false)
        }
    }

    @Test
    fun thatDebridLinkGetsAddedToDebridFileContentsWhenProviderIsMissing() {
        //given
        realDebridClient.stub {
            onBlocking { getCachedFiles(eq(magnet)) }.thenReturn(listOf(realDebridCachedFile))
        }
        realDebridClient.stub {
            onBlocking { isCached(any()) }.thenReturn(true)
        }

        val debridFileContentsWithoutRealDebridLink = debridFileContents.deepCopy()
        debridFileContentsWithoutRealDebridLink.debridLinks.removeFirst()

        //when
        val result = runBlocking { underTest.getCheckedLinks(file!!).first() }

        //then
        assertEquals(result.provider, DebridProvider.REAL_DEBRID)
    }

    @Test
    fun thatMissingLinkGetsReplacedWithCachedLinkWhenProviderHasFile() {
        //given
        realDebridClient.stub {
            onBlocking { getCachedFiles(eq(magnet)) }.thenReturn(listOf(realDebridCachedFile))
        }
        realDebridClient.stub {
            onBlocking { isCached(any()) }.thenReturn(true)
        }

        val debridFileContentsWithMissingRealDebridLink = debridFileContents.deepCopy()
        debridFileContentsWithMissingRealDebridLink.debridLinks = mutableListOf(
            MissingFile(DebridProvider.REAL_DEBRID, Instant.now(clock).minus(25, ChronoUnit.HOURS).toEpochMilli()),
            debridFileContents.debridLinks.last()
        )
        every { fileService.getDebridFileContents(any()) } returns debridFileContents.deepCopy()

        //when
        val result = runBlocking { underTest.getCheckedLinks(file!!).first() }

        //then
        assertEquals(result.provider, DebridProvider.REAL_DEBRID)
    }

    @Test
    fun thatRecentlyCheckedDebridFileDoesNotGetReChecked() {
        //given
        realDebridClient.stub {
            onBlocking { getCachedFiles(eq(magnet)) }.thenReturn(listOf(realDebridCachedFile))
        }
        realDebridClient.stub {
            onBlocking { isCached(any()) }.thenReturn(true)
        }
        premiumizeClient.stub {
            onBlocking { isCached(any()) }.thenReturn(true)
        }

        val debridFileContentsWithMissingRealDebridLink = debridFileContents.deepCopy()
        debridFileContentsWithMissingRealDebridLink.debridLinks = mutableListOf(
            MissingFile(DebridProvider.REAL_DEBRID, Instant.now(clock).minus(1, ChronoUnit.HOURS).toEpochMilli()),
            debridFileContents.debridLinks.last()
        )
        every { fileService.getDebridFileContents(any()) } returns debridFileContentsWithMissingRealDebridLink.deepCopy()
        given(linkCheckService.isLinkAlive(eq(premiumizeCachedFile))).willReturn(true)

        //when
        val result = runBlocking { underTest.getCheckedLinks(file!!).first() }

        //then
        assertEquals(DebridProvider.PREMIUMIZE, result.provider)
    }
    
    fun DebridFileContents.deepCopy() = DebridFileContentsDeserializer.deserialize(jacksonObjectMapper().writeValueAsString(this))

}