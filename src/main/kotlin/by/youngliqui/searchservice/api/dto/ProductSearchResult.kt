package by.youngliqui.searchservice.api.dto

import java.util.*

data class ProductSearchResult(
    val id: UUID,
    val name: String,
    val description: String?,
    val brand: String,
    val category: String,
    val price: Double,
    val highlights: Map<String, List<String>> = emptyMap()
)