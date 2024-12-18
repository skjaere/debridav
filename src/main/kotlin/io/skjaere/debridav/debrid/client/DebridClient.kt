package io.skjaere.debridav.debrid.client

import io.skjaere.debridav.fs.DebridProvider

interface DebridClient {
    fun getProvider(): DebridProvider
}