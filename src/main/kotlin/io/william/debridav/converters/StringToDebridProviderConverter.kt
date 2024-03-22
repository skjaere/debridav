package io.william.debridav.converters

import io.william.debridav.fs.DebridProvider
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToDebridProviderConverter : Converter<String, DebridProvider> {
    override fun convert(source: String): DebridProvider? {
        return DebridProvider.valueOf(source.uppercase())
    }
}