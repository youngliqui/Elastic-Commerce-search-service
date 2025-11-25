package by.youngliqui.searchservice.api.dto

import by.youngliqui.searchservice.document.ProductDocument

data class ProductSearchResponse(
    val content: List<ProductDocument>,
    val page: Int,

    val size: Int,
    val totalPages: Int,
    val totalElements: Long,
    val facets: List<Facet>
)