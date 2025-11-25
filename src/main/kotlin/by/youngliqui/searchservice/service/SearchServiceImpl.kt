package by.youngliqui.searchservice.service

import by.youngliqui.searchservice.api.dto.Facet
import by.youngliqui.searchservice.api.dto.FacetValue
import by.youngliqui.searchservice.api.dto.ProductSearchResponse
import by.youngliqui.searchservice.api.dto.SearchRequest
import by.youngliqui.searchservice.document.ProductDocument
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.stereotype.Service

@Service
class SearchServiceImpl(
    private val elasticsearchOperations: ElasticsearchOperations
) : SearchService {

    override fun searchProducts(request: SearchRequest, pageable: Pageable): ProductSearchResponse {
        // 1. Строим критерии поиска
        val criteria = buildSearchCriteria(request)
        val query = CriteriaQuery(criteria, pageable)

        // 2. Выполняем поиск
        val searchHits: SearchHits<ProductDocument> = elasticsearchOperations.search(query, ProductDocument::class.java)

        // 3. Получаем фасеты через отдельные запросы
        val facets = buildFacets(request)

        // 4. Строим ответ
        return buildSearchResponse(searchHits, pageable, facets)
    }

    /**
     * Строит критерии поиска на основе запроса
     */
    private fun buildSearchCriteria(request: SearchRequest): Criteria {
        val criteria = Criteria()

        request.query?.takeIf { it.isNotBlank() }?.let { query ->
            criteria.and(
                Criteria("name").matches(query)
                    .or("description").matches(query)
                    .or("brand").matches(query)
                    .or("category").matches(query)
            )
        }

        request.brands?.takeIf { it.isNotEmpty() }?.let { brands ->
            criteria.and(Criteria("brand").`in`(brands))
        }

        request.categories?.takeIf { it.isNotEmpty() }?.let { categories ->
            criteria.and(Criteria("category").`in`(categories))
        }

        if (request.priceFrom != null || request.priceTo != null) {
            val priceCriteria = Criteria("price")
            request.priceFrom?.let { priceCriteria.greaterThanEqual(it) }
            request.priceTo?.let { priceCriteria.lessThanEqual(it) }
            criteria.and(priceCriteria)
        }

        return criteria
    }

    /**
     * Строит фасеты через отдельные агрегационные запросы
     */
    private fun buildFacets(request: SearchRequest): List<Facet> {
        val facets = mutableListOf<Facet>()

        val brandsFacet = getBrandFacet(request)
        facets.add(brandsFacet)

        val categoriesFacet = getCategoryFacet(request)
        facets.add(categoriesFacet)

        return facets
    }

    /**
     * Получает фасет по брендам
     */
    private fun getBrandFacet(request: SearchRequest): Facet {
        val criteria = buildBaseCriteriaForFacets(request)
        val query = CriteriaQuery(criteria)

        val searchHits = elasticsearchOperations.search(query, ProductDocument::class.java)

        val brandCounts = searchHits.searchHits
            .map { it.content.brand }
            .groupingBy { it }
            .eachCount()

        val facetValues = brandCounts.map { (brand, count) ->
            FacetValue(brand, count.toLong())
        }.sortedByDescending { it.count }

        return Facet("brand", facetValues)
    }

    /**
     * Получает фасет по категориям
     */
    private fun getCategoryFacet(request: SearchRequest): Facet {
        val criteria = buildBaseCriteriaForFacets(request)
        val query = CriteriaQuery(criteria)

        val searchHits = elasticsearchOperations.search(query, ProductDocument::class.java)

        val categoryCounts = searchHits.searchHits
            .map { it.content.category }
            .groupingBy { it }
            .eachCount()

        val facetValues = categoryCounts.map { (category, count) ->
            FacetValue(category, count.toLong())
        }.sortedByDescending { it.count }

        return Facet("category", facetValues)
    }

    /**
     * Строит базовые критерии для фасетов (без учета фильтров по самому фасету)
     */
    private fun buildBaseCriteriaForFacets(request: SearchRequest): Criteria {
        val criteria = Criteria()

        request.query?.takeIf { it.isNotBlank() }?.let { query ->
            criteria.and(
                Criteria("name").matches(query)
                    .or("description").matches(query)
                    .or("brand").matches(query)
                    .or("category").matches(query)
            )
        }

        // Фильтрация по цене
        if (request.priceFrom != null || request.priceTo != null) {
            val priceCriteria = Criteria("price")
            request.priceFrom?.let { priceCriteria.greaterThanEqual(it) }
            request.priceTo?.let { priceCriteria.lessThanEqual(it) }
            criteria.and(priceCriteria)
        }

        return criteria
    }

    /**
     * Строит финальный ответ
     */
    private fun buildSearchResponse(
        searchHits: SearchHits<ProductDocument>,
        pageable: Pageable,
        facets: List<Facet>
    ): ProductSearchResponse {
        val products = searchHits.searchHits.map { it.content }
        val totalElements = searchHits.totalHits
        val totalPages = calculateTotalPages(totalElements, pageable.pageSize)

        return ProductSearchResponse(
            content = products,
            page = pageable.pageNumber,
            size = pageable.pageSize,
            totalPages = totalPages,
            totalElements = totalElements,
            facets = facets
        )
    }

    /**
     * Вычисляет общее количество страниц
     */
    private fun calculateTotalPages(totalElements: Long, pageSize: Int): Int {
        return if (pageSize > 0) {
            ((totalElements + pageSize - 1) / pageSize).toInt()
        } else {
            1
        }
    }
}