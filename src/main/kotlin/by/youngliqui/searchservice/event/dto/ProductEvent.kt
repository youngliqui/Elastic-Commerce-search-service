package by.youngliqui.searchservice.event.dto

import java.util.UUID

sealed interface ProductEvent

data class ProductCreatedEvent(
    val product: ProductResponse
) : ProductEvent

data class ProductUpdatedEvent(
    val product: ProductResponse
) : ProductEvent

data class ProductDeletedEvent(
    val id: UUID
) : ProductEvent