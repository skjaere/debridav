package io.william.debrid

import io.milton.annotations.ChildrenOf
import io.milton.annotations.ModifiedDate
import io.milton.annotations.Name
import io.milton.annotations.ResourceController
import io.milton.annotations.Root
import io.milton.resource.FileResource
import io.milton.resource.Resource
import java.time.Instant
import java.util.*


@ResourceController
class Controller {
    private val items = listOf(Item("hello.txt"))

    @Root
    fun getRoot(): Controller  = this

    @ChildrenOf
    fun getItems(root: Controller?): List<Item> {
        return items
    }

    @ModifiedDate
    fun modified(arg: Any): Date {
        return Date.from(Instant.now().minusSeconds(1))
    }
}