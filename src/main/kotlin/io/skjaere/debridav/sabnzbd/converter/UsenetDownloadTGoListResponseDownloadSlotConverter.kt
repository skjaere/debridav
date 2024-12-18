package io.skjaere.debridav.sabnzbd.converter

import io.skjaere.debridav.sabnzbd.ListResponseDownloadSlot
import io.skjaere.debridav.sabnzbd.UsenetDownload
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class CompletedUsenetDownloadTGoListResponseDownloadSlotConverter :
    Converter<UsenetDownload, ListResponseDownloadSlot> {
    override fun convert(source: UsenetDownload): ListResponseDownloadSlot? {
        val sizeInMb = source.size?.div((1 shl 20))
        val mbMissing = 0

        return ListResponseDownloadSlot(
            status = "Completed",
            size = bytesToHumanReadableSize(source.size!!),
            mb = "%".format(sizeInMb),
            cat = "defaultCat",
            directUnpack = "10/10",
            mbMissing = "%".format(mbMissing),
            percentage = "1.0",
            unpackOpts = "3",
            index = 0,
            nzoId = "nzoId",
            labels = listOf(),
            mbLeft = "0",
            script = "",
            filename = source.name!!,
            password = "",
            priority = "0",
            sizeLeft = "0",
            timeLeft = "0:00:00",
            avgAge = "1d"
        )
    }

    private fun bytesToHumanReadableSize(bytes: Long) = when {
        bytes >= 1 shl 30 -> "%.1f GB".format(bytes / (1 shl 30))
        bytes >= 1 shl 20 -> "%.1f MB".format(bytes / (1 shl 20))
        bytes >= 1 shl 10 -> "%.0f kB".format(bytes / (1 shl 10))
        else -> "$bytes bytes"
    }
}