package io.william.debrid

import io.william.debrid.fs.models.DebridFileContents
import org.mockito.ArgumentMatcher

class DebridContentsMatcher(private val contents: DebridFileContents) : ArgumentMatcher<DebridFileContents> {
    override fun matches(right: DebridFileContents): Boolean {
        return contents.link == right.link
    }
}