package by.youngliqui.searchservice.api.dto

data class SearchRequest(
    val query: String?,
    val brands: Set<String>?,
    val categories: Set<String>?,
    val priceFrom: Double?,
    val priceTo: Double?
)
