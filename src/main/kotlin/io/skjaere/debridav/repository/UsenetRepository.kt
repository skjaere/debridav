package io.skjaere.debridav.repository

import io.skjaere.debridav.qbittorrent.Category
import io.skjaere.debridav.sabnzbd.UsenetDownload
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface UsenetRepository : CrudRepository<UsenetDownload, Long> {
    fun getUsenetDownloadsByCompletedAndCategory(completed: Boolean, category: Category): List<UsenetDownload>
    fun getUsenetDownloadsByCompleted(completed: Boolean): MutableList<UsenetDownload>
    fun getAllByCreatedAfter(createdAfter: Date): List<UsenetDownload>

    @Modifying
    @Query("update UsenetDownload u set u.completed=true where u.debridId in (:debridIds)")
    fun setDownloadsToCompleted(debridIds: List<String>)
}