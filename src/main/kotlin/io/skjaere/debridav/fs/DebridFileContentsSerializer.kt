package io.skjaere.debridav.fs

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object DebridFileContentsSerializer :
    JsonContentPolymorphicSerializer<DebridFileContents>(DebridFileContents::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<DebridFileContents> {
        return when {
            element.jsonObject["type"] == null || element.jsonObject["type"]?.jsonPrimitive?.content == "TORRENT" ->
                DebridTorrentFileContents.serializer()

            else -> DebridUsenetFileContents.serializer()
        }
    }
}