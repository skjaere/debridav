package io.william.debrid

import io.milton.annotations.Name

data class Item(
    private val name: String
) {
    @Name
    fun getName(): String {
         return name
    }
}