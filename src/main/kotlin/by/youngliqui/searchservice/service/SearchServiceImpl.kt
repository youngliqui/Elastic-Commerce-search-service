package by.youngliqui.searchservice.service

import by.youngliqui.searchservice.api.dto.Facet
import by.youngliqui.searchservice.api.dto.FacetValue
import by.youngliqui.searchservice.api.dto.ProductSearchResponse
import by.youngliqui.searchservice.api.dto.SearchRequest
import by.youngliqui.searchservice.document.ProductDocument
import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery
import co.elastic.clients.elasticsearch._types.query_dsl.Operator
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType
import co.elastic.clients.json.JsonData
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations
import org.springframework.data.elasticsearch.client.elc.NativeQuery
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHits
import org.springframework.stereotype.Service

@Service
class SearchServiceImpl(
    private val elasticsearchOperations: ElasticsearchOperations
) : SearchService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun searchProducts(request: SearchRequest, pageable: Pageable): ProductSearchResponse {
        val query = buildNativeQuery(request, pageable)
        val searchHits = elasticsearchOperations.search(query, ProductDocument::class.java)
        return mapToResponse(searchHits, pageable)
    }

    private fun buildNativeQuery(request: SearchRequest, pageable: Pageable): NativeQuery {
        val builder = NativeQuery.builder()
            .withQuery(buildBoolQuery(request))
            .withPageable(pageable)

        // Агрегации (фасеты)
        builder.withAggregation("brands_facet", createTermAggregation("brand"))
        builder.withAggregation("categories_facet", createTermAggregation("category"))

        return builder.build()
    }

    private fun buildBoolQuery(request: SearchRequest): Query {
        return QueryBuilders.bool { b ->
            // 1. Полнотекстовый поиск
            request.query?.takeIf { it.isNotBlank() }?.let { q ->
                b.must(createMultiMatchQuery(q))
            }

            // 2. Фильтры
            applyFilters(b, request)
            b
        }
    }

    private fun createMultiMatchQuery(searchText: String): Query {
        return QueryBuilders.multiMatch { mm ->
            mm.query(searchText)
                .fields("name^3", "description")
                .fuzziness("AUTO")
                .operator(Operator.And)
                .type(TextQueryType.BestFields)
        }
    }

    private fun applyFilters(b: BoolQuery.Builder, request: SearchRequest) {
        // Фильтр: Бренды
        if (!request.brands.isNullOrEmpty()) {
            b.filter { f ->
                f.terms { t ->
                    t.field("brand")
                        .terms { v -> v.value(request.brands.map { FieldValue.of(it) }) }
                }
            }
        }

        // Фильтр: Категории
        if (!request.categories.isNullOrEmpty()) {
            b.filter { f ->
                f.terms { t ->
                    t.field("category")
                        .terms { v -> v.value(request.categories.map { FieldValue.of(it) }) }
                }
            }
        }

        // Фильтр: Цена
        if (request.priceFrom != null || request.priceTo != null) {
            b.filter { f ->
                f.range { r ->
                    r.field("price")
                    // Используем JsonData.of() для совместимости с API 8.x
                    request.priceFrom?.let { r.gte(JsonData.of(it)) }
                    request.priceTo?.let { r.lte(JsonData.of(it)) }
                    r
                }
            }
        }
    }

    private fun createTermAggregation(fieldName: String): Aggregation {
        return Aggregation.of { a ->
            a.terms { t -> t.field(fieldName).size(20) }
        }
    }

    private fun mapToResponse(
        searchHits: SearchHits<ProductDocument>,
        pageable: Pageable
    ): ProductSearchResponse {
        val products = searchHits.searchHits.map { it.content }
        val totalElements = searchHits.totalHits
        val totalPages = if (pageable.pageSize > 0)
            ((totalElements + pageable.pageSize - 1) / pageable.pageSize).toInt()
        else 1

        val aggregations = searchHits.aggregations as? ElasticsearchAggregations

        val facets = listOf(
            Facet("brand", extractFacetValues(aggregations, "brands_facet")),
            Facet("category", extractFacetValues(aggregations, "categories_facet"))
        )

        return ProductSearchResponse(
            content = products,
            page = pageable.pageNumber,
            size = pageable.pageSize,
            totalPages = totalPages,
            totalElements = totalElements,
            facets = facets
        )
    }

    private fun extractFacetValues(
        aggregations: ElasticsearchAggregations?,
        facetName: String
    ): List<FacetValue> {
        if (aggregations == null) return emptyList()

        val elasticAgg = aggregations.get(facetName) ?: return emptyList()
        val aggregate = elasticAgg.aggregation().aggregate

        if (!aggregate.isSterms) return emptyList()

        return aggregate.sterms().buckets().array().map { bucket ->
            FacetValue(
                value = bucket.key().stringValue(),
                count = bucket.docCount()
            )
        }
    }
}
