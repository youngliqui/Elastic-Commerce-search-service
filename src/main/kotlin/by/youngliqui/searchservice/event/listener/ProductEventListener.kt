package by.youngliqui.searchservice.event.listener

import by.youngliqui.searchservice.event.dto.ProductCreatedEvent
import by.youngliqui.searchservice.event.dto.ProductDeletedEvent
import by.youngliqui.searchservice.event.dto.ProductUpdatedEvent
import by.youngliqui.searchservice.service.IndexingService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class ProductEventListener(
    private val indexingService: IndexingService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val QUEUE_NAME = "product.events.queue"
    }

    @RabbitListener(queues = [QUEUE_NAME])
    fun handleProductEvent(event: Any) {
        log.info("Получено событие: $event")
        when (event) {
            is ProductCreatedEvent -> indexingService.indexProduct(event.product)
            is ProductUpdatedEvent -> indexingService.indexProduct(event.product)
            is ProductDeletedEvent -> indexingService.deleteProductById(event.id)
            else -> log.warn("Получен неизвестный тип события: ${event::class.java.name}")
        }
    }
}