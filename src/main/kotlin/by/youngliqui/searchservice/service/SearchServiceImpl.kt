package by.youngliqui.searchservice.service

import by.youngliqui.searchservice.api.dto.Facet
import by.youngliqui.searchservice.api.dto.FacetValue
import by.youngliqui.searchservice.api.dto.ProductSearchResponse
import by.youngliqui.searchservice.api.dto.ProductSearchResult
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
import org.springframework.data.elasticsearch.core.query.HighlightQuery
import org.springframework.data.elasticsearch.core.query.highlight.Highlight
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters
import org.springframework.stereotype.Service

@Service
class SearchServiceImpl(
    private val elasticsearchOperations: ElasticsearchOperations
) : SearchService {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Метод поиска.
     * Выполняет построение запроса, обращение к ElasticSearch и маппинг результата.
     */
    override fun searchProducts(request: SearchRequest, pageable: Pageable): ProductSearchResponse {
        val query = buildNativeQuery(request, pageable)
        val searchHits = elasticsearchOperations.search(query, ProductDocument::class.java)
        return mapToResponse(searchHits, pageable)
    }

    /**
     * Собирает финальный NativeQuery объект из фильтров, подсветки и агрегаций.
     */
    private fun buildNativeQuery(request: SearchRequest, pageable: Pageable): NativeQuery {
        val builder = NativeQuery.builder()
            .withQuery(buildBoolQuery(request))
            .withPageable(pageable)

        builder.withHighlightQuery(createHighlightQuery())
        builder.withAggregation("brands_facet", createTermAggregation("brand.raw"))
        builder.withAggregation("categories_facet", createTermAggregation("category.raw"))

        return builder.build()
    }

    /**
     * Строит логический запрос (BOOL Query), объединяющий полнотекстовый поиск и жесткие фильтры.
     */
    private fun buildBoolQuery(request: SearchRequest): Query {
        return QueryBuilders.bool { b ->
            request.query?.takeIf { it.isNotBlank() }?.let { q ->
                b.must(createMultiMatchQuery(q))
            }

            applyFilters(b, request)
            b
        }
    }

    /**
     * Создает MultiMatch запрос для поиска по текстовым полям с учетом опечаток.
     */
    private fun createMultiMatchQuery(searchText: String): Query {
        return QueryBuilders.multiMatch { mm ->
            mm.query(searchText)
                .fields("name^3", "description")
                .fuzziness("AUTO")
                .operator(Operator.And)
                .type(TextQueryType.BestFields)
        }
    }

    /**
     * Настраивает параметры подсветки (Highlighting).
     * Оборачивает найденные вхождения в теги <em>...</em>.
     */
    private fun createHighlightQuery(): HighlightQuery {
        val highlight = Highlight(
            listOf(
                HighlightField(
                    "name",
                    HighlightFieldParameters.builder().withPreTags("<em>").withPostTags("</em>").build()
                ),
                HighlightField(
                    "description",
                    HighlightFieldParameters.builder().withPreTags("<em>").withPostTags("</em>").build()
                )
            )
        )
        return HighlightQuery(highlight, ProductDocument::class.java)
    }

    /**
     * Применяет фильтры к запросу (Brands, Categories, Price Range).
     * Фильтры не влияют на релевантность (score), только отсекают лишнее.
     */
    private fun applyFilters(b: BoolQuery.Builder, request: SearchRequest) {
        if (!request.brands.isNullOrEmpty()) {
            b.filter { f ->
                f.terms { t ->
                    t.field("brand.raw")
                        .terms { v -> v.value(request.brands.map { FieldValue.of(it) }) }
                }
            }
        }

        if (!request.categories.isNullOrEmpty()) {
            b.filter { f ->
                f.terms { t ->
                    t.field("category.raw")
                        .terms { v -> v.value(request.categories.map { FieldValue.of(it) }) }
                }
            }
        }

        if (request.priceFrom != null || request.priceTo != null) {
            b.filter { f ->
                f.range { r ->
                    r.field("price")
                    request.priceFrom?.let { r.gte(JsonData.of(it)) }
                    request.priceTo?.let { r.lte(JsonData.of(it)) }
                    r
                }
            }
        }
    }

    /**
     * Создает агрегацию по терминам (Terms Aggregation) для построения фасетов.
     * Возвращает топ-20 значений.
     */
    private fun createTermAggregation(fieldName: String): Aggregation {
        return Aggregation.of { a ->
            a.terms { t -> t.field(fieldName).size(20) }
        }
    }

    /**
     * Преобразует сырой ответ ElasticSearch (SearchHits) в DTO ответа API.
     * Включает контент, пагинацию и вычисленные фасеты.
     */
    private fun mapToResponse(
        searchHits: SearchHits<ProductDocument>,
        pageable: Pageable
    ): ProductSearchResponse {
        val products = searchHits.searchHits.map { hit ->
            val doc = hit.content
            val highlights = hit.highlightFields

            ProductSearchResult(
                id = doc.id,
                name = doc.name,
                description = doc.description,
                brand = doc.brand,
                category = doc.category,
                price = doc.price,
                highlights = highlights
            )
        }

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

    /**
     * Извлекает бакеты (группы) из результата агрегации ElasticSearch.
     */
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
