package by.youngliqui.searchservice.service

import by.youngliqui.searchservice.event.dto.ProductResponse
import by.youngliqui.searchservice.mapper.toDocument
import by.youngliqui.searchservice.repository.ProductSearchRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IndexingServiceImpl(
    private val repository: ProductSearchRepository,
) : IndexingService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun indexProduct(productDto: ProductResponse) {
        val document = productDto.toDocument()
        repository.save(document)
        log.info("Товар с ID '${document.id}' успешно проиндексирован/обновлен.")
    }

    override fun deleteProductById(id: UUID) {
        repository.deleteById(id)
        log.info("Товар с ID '$id' удален из индекса.")
    }
}