package io.william.debridav.qbittorrent

import io.william.debridav.debrid.DebridClient
import io.william.debridav.debrid.DebridResponse
import io.william.debridav.fs.FileService
import io.william.debridav.repository.CategoryRepository
import io.william.debridav.repository.TorrentFileRepository
import io.william.debridav.repository.TorrentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant


@Service
class TorrentService(
        private val debridClient: DebridClient,
        private val fileService: FileService,
        private val torrentRepository: TorrentRepository,
        private val torrentFileRepository: TorrentFileRepository,
        private val categoryRepository: CategoryRepository
) {
    private val logger = LoggerFactory.getLogger(TorrentService::class.java)

    fun addTorrent(category: String, magnet: String): Boolean {
        if (debridClient.isCached(magnet)) {
            debridClient.getDirectDownloadLink(magnet).let { cachedFiles ->
                if (cachedFiles.isEmpty()) {
                    logger.warn("Received empty list of files from debrid client")
                    return false
                }
                createTorrent(cachedFiles, category)
                cachedFiles.forEach {
                    createFile(it, magnet)
                }
                return true
            }
        } else {
            logger.info("$magnet is not cached")
            return false
        }
    }

    fun createFile(
            content: DebridResponse,
            magnet: String
    ) {
        val createRequest = FileService.CreateFileRequest(
                content.path,
                content.size,
                content.link
        )
        fileService.createDebridFile(createRequest, magnet, null)
    }

    fun createTorrent(content: List<DebridResponse>, categoryName: String) {
        val torrent = Torrent()
        torrent.category = categoryRepository.findByName(categoryName)
                ?: run { createCategory(categoryName) }
        torrent.name = content.first().path.split("/").first()
        torrent.created = Instant.now()
        torrent.hash = generateHash(torrent)
        torrent.savePath = "${torrent.category!!.downloadPath}/"
        torrent.files = content.map {
            val torrentFile = TorrentFile()
            torrentFile.fileName = it.path
            torrentFile.size = it.size
            torrentFile.path = it.path
            torrentFileRepository.save(torrentFile)
        }
        torrentRepository.save(torrent)
    }

    fun getTorrentsByCategory(categoryName: String): List<Torrent> {
        return categoryRepository.findByName(categoryName)?.let { category ->
            torrentRepository.findByCategory(category)
        } ?: emptyList()
    }

    fun createCategory(categoryName: String): Category {
        val category = Category()
        category.name = categoryName
        category.downloadPath = "/downloads"
        return categoryRepository.save(category)

    }

    fun getCategories(): List<Category> {
        return categoryRepository.findAll().toMutableList()
    }

    fun getTorrentByHash(hash: String): Torrent? {
        return torrentRepository.getByHash(hash)
    }

    fun deleteTorrentByHash(hash: String): Boolean {
        return torrentRepository.getByHash(hash.uppercase())?.let {
            torrentRepository.delete(it)
            true
        } ?: false
    }


    private fun generateHash(torrent: Torrent): String {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-1")
        var bytes: ByteArray = "${torrent.id}${torrent.name}${torrent.created}${torrent.category}".toByteArray(Charsets.UTF_8)
        digest.update(bytes, 0, bytes.size)
        bytes = digest.digest()

        return bytesToHex(bytes)
    }

    protected val hexArray: CharArray = "0123456789ABCDEF".toCharArray()

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
