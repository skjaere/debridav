package io.william.debrid.repository

import io.william.debrid.qbittorrent.Category
import org.springframework.data.repository.CrudRepository

interface CategoryRepository: CrudRepository<Category, Long> {
    fun findByName(name: String): Category?
}