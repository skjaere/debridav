package io.william.debridav.repository

import io.william.debridav.qbittorrent.Category
import org.springframework.data.repository.CrudRepository

interface CategoryRepository : CrudRepository<Category, Long> {
    fun findByName(name: String): Category?
}