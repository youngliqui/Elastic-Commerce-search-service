package by.youngliqui.searchservice.service

import by.youngliqui.searchservice.api.dto.ProductSearchResponse
import by.youngliqui.searchservice.api.dto.SearchRequest
import org.springframework.data.domain.Pageable

interface SearchService {
    fun searchProducts(request: SearchRequest, pageable: Pageable): ProductSearchResponse
}