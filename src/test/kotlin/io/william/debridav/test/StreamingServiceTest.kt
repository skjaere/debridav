package io.william.debridav.test

import io.milton.http.Range
import io.william.debridav.StreamingService
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.fs.DebridLink
import io.william.debridav.fs.DebridProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.given
import org.mockito.kotlin.spy
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection

class StreamingServiceTest {

    @Test
    fun returnsDeadLinkWhenConnectionFails() {
        //given
        val debridFileContents = DebridFileContents(
                "film.mkv",
                100,
                100,
                "magnet",
                mutableListOf(DebridLink(DebridProvider.REAL_DEBRID, "http://localhost/film.mkv"))
        )
        val streamingService = spy(StreamingService())
        val mockedConnection = mock(HttpURLConnection::class.java)
        given(mockedConnection.responseCode).willReturn(404)
        doReturn(mockedConnection).`when`(streamingService).openConnection("http://localhost/film.mkv")

        //when
        val result = streamingService.streamDebridLink(debridFileContents.debridLinks.first(), Range(0, 1), 1, ByteArrayOutputStream())

        //then
        assertEquals(StreamingService.Result.DEAD_LINK, result)
    }
}