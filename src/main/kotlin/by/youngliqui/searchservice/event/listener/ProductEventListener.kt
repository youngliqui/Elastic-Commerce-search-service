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
    fun handleProductCreated(event: ProductCreatedEvent) {
        log.info("Получено событие ProductCreatedEvent: $event")
        indexingService.indexProduct(event.product)
    }

    @RabbitListener(queues = [QUEUE_NAME])
    fun handleProductUpdated(event: ProductUpdatedEvent) {
        log.info("Получено событие ProductUpdatedEvent: $event")
        indexingService.indexProduct(event.product)
    }

    @RabbitListener(queues = [QUEUE_NAME])
    fun handleProductDeleted(event: ProductDeletedEvent) {
        log.info("Получено событие ProductDeletedEvent: $event")
        indexingService.deleteProductById(event.id)
    }
}