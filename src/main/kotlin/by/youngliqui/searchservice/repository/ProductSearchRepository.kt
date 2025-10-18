package by.youngliqui.searchservice.repository

import by.youngliqui.searchservice.document.ProductDocument
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductSearchRepository: ElasticsearchRepository<ProductDocument, UUID>