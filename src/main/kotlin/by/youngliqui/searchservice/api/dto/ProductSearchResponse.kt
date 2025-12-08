package by.youngliqui.searchservice.api.dto


data class ProductSearchResponse(
    val content: List<ProductSearchResult>,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val totalElements: Long,
    val facets: List<Facet>
)