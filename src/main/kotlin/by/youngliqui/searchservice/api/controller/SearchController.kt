package by.youngliqui.searchservice.api.controller

import by.youngliqui.searchservice.api.dto.ProductSearchResponse
import by.youngliqui.searchservice.api.dto.SearchRequest
import by.youngliqui.searchservice.service.SearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search API", description = "API для поиска товаров через Elasticsearch")
class SearchController(
    private val searchService: SearchService
) {

    @GetMapping
    @Operation(
        summary = "Поиск товаров",
        description = "Полнотекстовый поиск с фильтрацией по бренду, категории, цене и фасетами"
    )
    fun searchProducts(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) brand: Set<String>?,
        @RequestParam(required = false) category: Set<String>?,
        @RequestParam(required = false) priceFrom: Double?,
        @RequestParam(required = false) priceTo: Double?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "price,asc") sort: List<String>
    ): ResponseEntity<ProductSearchResponse> {

        val searchRequest = SearchRequest(
            query = q,
            brands = brand,
            categories = category,
            priceFrom = priceFrom,
            priceTo = priceTo
        )

        val sortOrders = sort.map { sortParam ->
            val parts = sortParam.split(",")
            val direction = if (parts.size > 1 && parts[1].equals("desc", ignoreCase = true)) {
                Sort.Direction.DESC
            } else {
                Sort.Direction.ASC
            }
            Sort.Order(direction, parts[0])
        }

        val pageable = PageRequest.of(page, size, Sort.by(sortOrders))

        val result = searchService.searchProducts(searchRequest, pageable)
        return ResponseEntity.ok(result)
    }
}