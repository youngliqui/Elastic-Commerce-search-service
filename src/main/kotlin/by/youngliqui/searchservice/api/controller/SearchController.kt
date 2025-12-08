package by.youngliqui.searchservice.api.controller

import by.youngliqui.searchservice.api.dto.ProductSearchResponse
import by.youngliqui.searchservice.api.dto.SearchRequest
import by.youngliqui.searchservice.service.SearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
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
        @PageableDefault(size = 20, sort = ["price"], direction = Sort.Direction.ASC) pageable: Pageable
    ): ResponseEntity<ProductSearchResponse> {

        val searchRequest = SearchRequest(
            query = q,
            brands = brand,
            categories = category,
            priceFrom = priceFrom,
            priceTo = priceTo
        )

        val result = searchService.searchProducts(searchRequest, pageable)
        return ResponseEntity.ok(result)
    }
}