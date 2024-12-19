package io.skjaere.debridav.sabnzbd.converter

import io.skjaere.debridav.sabnzbd.ListResponseDownloadSlot
import io.skjaere.debridav.sabnzbd.UsenetDownload
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class UsenetDownloadToListResponseDownloadSlotConverter : Converter<UsenetDownload, ListResponseDownloadSlot> {
    override fun convert(source: UsenetDownload): ListResponseDownloadSlot? {
        val percentageRemaining = 1.0 - (source.percentCompleted ?: 0.0)
        val bytesLeft = source.size?.times(percentageRemaining) ?: 0.0
        val mbLeft = bytesLeft.div(1024).times(percentageRemaining)
        val mb = source.size?.div(1024) ?: 0


        return ListResponseDownloadSlot(
            status = if (source.completed!!) "completed" else "downloading",
            index = 0,
            password = "",
            avgAge = "1h",
            script = "",
            directUnpack = "",
            mb = "$mb",
            mbLeft = "$mbLeft",
            mbMissing = "0",
            size = source.size.toString(),
            sizeLeft = "$bytesLeft",
            filename = source.name!!,
            labels = listOf("label"),
            priority = "0",
            cat = "tv", //TODO: fixme
            timeLeft = "0:10:00",
            percentage = (source.percentCompleted?.times(100))?.toInt().toString(),
            nzoId = "${source.id}",
            unpackOpts = "3"
        )
    }
}