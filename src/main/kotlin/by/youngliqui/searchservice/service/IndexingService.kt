package by.youngliqui.searchservice.service

import by.youngliqui.searchservice.event.dto.ProductResponse
import java.util.UUID

interface IndexingService {
    fun indexProduct(productDto: ProductResponse)
    fun deleteProductById(id: UUID)
}