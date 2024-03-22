package io.william.debridav

import io.william.debridav.debrid.DebridResponse
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.fs.DebridLink
import io.william.debridav.fs.DebridProvider

val debridFileContents = DebridFileContents(
        "a/b/c",
        100,
        1000,
        "magnet",
        mutableListOf(
                DebridLink(
                        DebridProvider.PREMIUMIZE,
                        "http://localhost:999/deadLink",
                )
        )
)

val directDownloadResponse = listOf(DebridResponse(
        "a/b/c",
        1000,
        "video/mp4",
        "https://test.com/video.mkv",
))