package io.william.debridav.test

import io.william.debridav.debrid.DebridResponse
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.fs.DebridLink
import io.william.debridav.fs.DebridProvider

val magnet = "magnet:?xt=urn:btih:hash&dn=test&tr="

val debridFileContents = DebridFileContents(
        "a/b/c/movie.mkv",
        100,
        1000,
        magnet,
        mutableListOf(
                DebridLink(
                        DebridProvider.PREMIUMIZE,
                        "http://localhost:999/deadLink",
                )
        )
)

val directDownloadResponse = listOf(DebridResponse(
        "a/b/c/movie.mkv",
        1000,
        "video/mp4",
        "https://test.com/video.mkv",
))