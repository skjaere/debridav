package io.william.debrid.qbittorrent

import io.william.debrid.fs.*
import io.william.debrid.premiumize.DirectDownloadResponse
import io.william.debrid.premiumize.PremiumizeClient
import io.william.debrid.repository.CategoryRepository
import io.william.debrid.repository.TorrentFileRepository
import io.william.debrid.repository.TorrentRepository
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant


@Service
class TorrentService(
    private val premiumizeClient: PremiumizeClient,
    private val fileService: FileService,
    private val torrentRepository: TorrentRepository,
    private val torrentFileRepository: TorrentFileRepository,
    private val categoryRepository: CategoryRepository
) {
    fun addTorrent(category: String, magnet: String) {
        if (premiumizeClient.isCached(magnet)) {
            premiumizeClient.getDirectDownloadLink(magnet)?.let { cachedTorrent ->
                createTorrent(cachedTorrent, category)
                cachedTorrent.content.forEach {
                    //if(it.streamLink == it.link) {
                        createFile(it)
                    //}
                }
            }
        }
    }

    fun createFile(content: DirectDownloadResponse.Content) {
        val createRequest = FileService.CreateFileRequest(
            "/downloads",
            FileService.CreateFileRequest.Type.DEBRID,
            FileService.CreateFileRequest.File(
                content.path,
                content.size,
                content.link,
                content.streamLink
            )
        )
        fileService.createDebridFile(createRequest)
    }

    fun createTorrent(content: DirectDownloadResponse, categoryName: String) {
        val torrent = Torrent()
        torrent.category = categoryRepository.findByName(categoryName)
            ?: run {
                val newCategory = Category()
                newCategory.name = categoryName
                newCategory.downloadPath = "/downloads"
                categoryRepository.save(newCategory)
            }
        torrent.name = content.filename.split("/").first()
        torrent.created = Instant.now()
        torrent.hash = generateHash(torrent)
        torrent.savePath = "${torrent.category!!.downloadPath}/"
        torrent.files = content.content.map {
            val torrentFile = TorrentFile()
            torrentFile.fileName = it.path
            torrentFile.size = it.size
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

    fun generateHash(torrent: Torrent): String {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-1")
        var bytes: ByteArray = "${torrent.id}${torrent.name}${torrent.created}${torrent.category}".toByteArray(Charsets.UTF_8)
        digest.update(bytes, 0, bytes.size)
        bytes = digest.digest()

        return bytesToHex(bytes)
    }
    protected val hexArray: CharArray = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = hexArray[v ushr 4]
            hexChars[j * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }
}
