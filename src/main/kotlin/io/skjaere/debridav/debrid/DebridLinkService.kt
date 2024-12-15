package io.skjaere.debridav.debrid

import io.skjaere.debridav.LinkCheckService
import io.skjaere.debridav.configuration.DebridavConfiguration
import io.skjaere.debridav.debrid.client.DebridClient
import io.skjaere.debridav.debrid.client.model.ClientErrorGetCachedFilesResponse
import io.skjaere.debridav.debrid.client.model.GetCachedFilesResponse
import io.skjaere.debridav.debrid.client.model.NetworkErrorGetCachedFilesResponse
import io.skjaere.debridav.debrid.client.model.NotCachedGetCachedFilesResponse
import io.skjaere.debridav.debrid.client.model.ProviderErrorGetCachedFilesResponse
import io.skjaere.debridav.debrid.client.model.SuccessfulGetCachedFilesResponse
import io.skjaere.debridav.debrid.model.CachedFile
import io.skjaere.debridav.debrid.model.ClientError
import io.skjaere.debridav.debrid.model.DebridFile
import io.skjaere.debridav.debrid.model.MissingFile
import io.skjaere.debridav.debrid.model.NetworkError
import io.skjaere.debridav.debrid.model.ProviderError
import io.skjaere.debridav.fs.DebridFileContents
import io.skjaere.debridav.fs.DebridProvider
import io.skjaere.debridav.fs.FileService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.Clock
import java.time.Instant

@Service
class DebridLinkService(
    private val debridService: DebridService,
    private val linkCheckService: LinkCheckService,
    private val fileService: FileService,
    private val debridavConfiguration: DebridavConfiguration,
    private val debridClients: List<DebridClient>,
    private val clock: Clock
) {
    private val logger = LoggerFactory.getLogger(DebridLinkService::class.java)

    suspend fun getCheckedLinks(file: File): Flow<CachedFile> {
        val debridFileContents = fileService.getDebridFileContents(file)
        return getFlowOfDebridLinks(debridFileContents)
            .catch { e -> logger.error("Uncaught exception encountered while getting links", e) }
            .transformWhile { debridLink ->
                if (debridLink !is NetworkError) {
                    updateContentsOfDebridFile(debridFileContents, debridLink, file)
                }
                if (debridLink is CachedFile) {
                    emit(debridLink)
                }
                debridLink !is CachedFile
            }
    }

    private suspend fun getFlowOfDebridLinks(debridFileContents: DebridFileContents): Flow<DebridFile> = flow {
        debridavConfiguration.debridClients.map { debridProvider ->
            debridFileContents.debridLinks
                .firstOrNull { it.provider == debridProvider }
                ?.let { debridFile -> emitDebridFile(debridFile, debridFileContents, debridProvider) }
                ?: run { emit(getFreshDebridLink(debridFileContents, debridClients.getClient(debridProvider))) }
        }
    }

    private suspend fun getFreshDebridLink(
        debridFileContents: DebridFileContents,
        debridClient: DebridClient
    ): DebridFile {
        return debridFileContents.debridLinks
            .firstOrNull { it.provider == debridClient.getProvider() }
            ?.let { debridFile ->
                if (debridFile is CachedFile) {
                    debridClient.getStreamableLink(debridFileContents.magnet, debridFile)
                        ?.let { link ->
                            debridFile.withNewLink(link)
                        }
                } else null
            } ?: run {
            if (debridClient.isCached(debridFileContents.magnet)) {
                return debridService.getCachedFiles(debridFileContents.magnet, listOf(debridClient))
                    .map { response ->
                        mapResponseToDebridFile(response, debridFileContents, debridClient)
                    }.first()
            } else {
                MissingFile(debridClient.getProvider(), Instant.now(clock).toEpochMilli())
            }
        }
    }

    private fun mapResponseToDebridFile(
        response: GetCachedFilesResponse,
        debridFileContents: DebridFileContents,
        debridClient: DebridClient
    ) = when (response) {
        is SuccessfulGetCachedFilesResponse -> response.getCachedFiles()
            .first { fileMatches(it, debridFileContents) }

        is ProviderErrorGetCachedFilesResponse -> ProviderError(
            debridClient.getProvider(),
            Instant.now(clock).toEpochMilli()
        )

        is NotCachedGetCachedFilesResponse -> MissingFile(
            debridClient.getProvider(),
            Instant.now(clock).toEpochMilli()
        )

        is NetworkErrorGetCachedFilesResponse -> NetworkError(
            debridClient.getProvider(),
            Instant.now(clock).toEpochMilli()
        )

        is ClientErrorGetCachedFilesResponse -> ClientError(
            debridClient.getProvider(),
            Instant.now(clock).toEpochMilli(),
        )
    }

    private suspend fun FlowCollector<DebridFile>.emitDebridFile(
        debridFile: DebridFile,
        debridFileContents: DebridFileContents,
        debridProvider: DebridProvider
    ) {
        when (debridFile) {
            is CachedFile -> emitWorkingLink(debridFile, debridFileContents, debridProvider)
            is MissingFile -> emitRefreshedResult(debridFile, debridFileContents, debridProvider)
            is ProviderError -> emitRefreshedResult(debridFile, debridFileContents, debridProvider)
            is ClientError -> emitRefreshedResult(debridFile, debridFileContents, debridProvider)
            is NetworkError -> emit(
                NetworkError(
                    debridProvider,
                    Instant.now(clock).toEpochMilli()
                )
            )
        }
    }

    private suspend fun FlowCollector<DebridFile>.emitRefreshedResult(
        debridFile: DebridFile,
        debridFileContents: DebridFileContents,
        debridProvider: DebridProvider
    ) {
        if (linkShouldBeReChecked(debridFile)) {
            emit(getFreshDebridLink(debridFileContents, debridClients.getClient(debridProvider)))
        } else {
            emit(debridFile)
        }
    }

    private suspend fun FlowCollector<DebridFile>.emitWorkingLink(
        debridFile: CachedFile,
        debridFileContents: DebridFileContents,
        debridProvider: DebridProvider
    ) {
        if (linkCheckService.isLinkAlive(debridFile.link)) {
            emit(debridFile)
        } else {
            emit(getFreshDebridLink(debridFileContents, debridClients.getClient(debridProvider)))
        }
    }

    private fun updateContentsOfDebridFile(
        debridFileContents: DebridFileContents,
        debridLink: DebridFile,
        file: File
    ) {
        debridFileContents.replaceOrAddDebridLink(debridLink)
        fileService.writeContentsToFile(file, debridFileContents)
    }

    private fun fileMatches(
        it: CachedFile,
        debridFileContents: DebridFileContents
    ) = it.path.split("/").last() == debridFileContents.originalPath.split("/").last()

    private fun linkShouldBeReChecked(debridFile: DebridFile): Boolean {
        return when (debridFile) {
            is MissingFile -> debridavConfiguration.waitAfterMissing
            is ProviderError -> debridavConfiguration.waitAfterProviderError
            is NetworkError -> debridavConfiguration.waitAfterNetworkError
            is ClientError -> debridavConfiguration.waitAfterClientError
            is CachedFile -> null
        }?.let {
            return Instant.ofEpochMilli(debridFile.lastChecked)
                .isBefore(Instant.now(clock).minus(it))
        } ?: false
    }
}
