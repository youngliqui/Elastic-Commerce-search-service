package by.youngliqui.searchservice.event.dto

import java.util.UUID

data class ProductResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val price: Double,
    val brand: String,
    val category: String
)
