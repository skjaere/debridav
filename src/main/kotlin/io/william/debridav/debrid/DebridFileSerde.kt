package io.william.debridav.debrid

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.william.debridav.fs.DebridFileContents
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

/*object DebridFileSerializer : JsonContentPolymorphicSerializer<DebridFile>(DebridFile::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<DebridFile> {
        return if(element.jsonObject["link"] == null) MissingFile.serializer()
        else CachedFile.serializer()
    }
}

object DebridFileContentsSerializer : JsonObjectS<DebridFile>(DebridFile::class) {}*/

object DebridFileContentsDeserializer {
    val objectMapper = jacksonObjectMapper()
    fun deserialize(json: String): DebridFileContents {
        val jsonNode = objectMapper.readTree(json)

        val debridFiles =jsonNode.get("debridLinks").map {
            if(it["link"] != null) {
                objectMapper.convertValue(it, CachedFile::class.java) as CachedFile
            } else {
                objectMapper.convertValue(it, MissingFile::class.java) as MissingFile
            }
        }
        return DebridFileContents(
            jsonNode.get("originalPath").asText(),
            jsonNode.get("size").asLong(),
            jsonNode.get("modified").asLong(),
            jsonNode.get("magnet").asText(),
            debridFiles.toMutableList()
        )
    }
}