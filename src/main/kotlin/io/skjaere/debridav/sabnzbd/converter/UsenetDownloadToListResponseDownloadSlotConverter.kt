package io.skjaere.debridav.sabnzbd.converter

import io.skjaere.debridav.sabnzbd.ListResponseDownloadSlot
import io.skjaere.debridav.sabnzbd.UsenetDownload
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class UsenetDownloadToListResponseDownloadSlotConverter : Converter<UsenetDownload, ListResponseDownloadSlot> {
    override fun convert(source: UsenetDownload): ListResponseDownloadSlot? {
        return ListResponseDownloadSlot(
            status = if (source.completed!!) "completed" else "downloading",
            index = 0,
            password = "",
            avgAge = "1h",
            script = "",
            directUnpack = "",
            mb = (source.size?.div(1024)).toString(),
            mbLeft = ((source.size?.times((1 - source.percentCompleted!!)))?.div(1024)).toString(),
            mbMissing = "0",
            size = source.size.toString(),
            sizeLeft = (source.size?.times((1 - source.percentCompleted!!))).toString(),
            filename = source.name!!,
            labels = listOf("label"),
            priority = "0",
            cat = "category",
            timeLeft = "1h",
            percentage = source.percentCompleted.toString(),
            nzoId = "1",
            unpackOpts = "3"
        )
    }
}