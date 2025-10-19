package by.youngliqui.searchservice.config

import by.youngliqui.searchservice.event.dto.ProductCreatedEvent
import by.youngliqui.searchservice.event.dto.ProductDeletedEvent
import by.youngliqui.searchservice.event.dto.ProductUpdatedEvent
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.DefaultClassMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val EXCHANGE_NAME = "products.exchange"
        const val QUEUE_NAME = "product.events.queue"
        const val ROUTING_KEY_PREFIX = "products.event"
    }

    @Bean
    fun productsExchange(): TopicExchange = TopicExchange(EXCHANGE_NAME)

    @Bean
    fun productEventsQueue(): Queue = Queue(QUEUE_NAME)

    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding =
        BindingBuilder.bind(queue)
            .to(exchange)
            .with("$ROUTING_KEY_PREFIX.#")

    @Bean
    fun classMapper(): DefaultClassMapper {
        return DefaultClassMapper().apply {
            setIdClassMapping(
                mapOf(
                    "product_created" to ProductCreatedEvent::class.java,
                    "product_updated" to ProductUpdatedEvent::class.java,
                    "product_deleted" to ProductDeletedEvent::class.java
                )
            )
            setTrustedPackages("*")
        }
    }

    @Bean
    fun jackson2JsonMessageConverter(classMapper: DefaultClassMapper): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter().apply {
            setClassMapper(classMapper)
        }
    }

    @Bean
    fun rabbitTemplate(
        configurer: RabbitTemplateConfigurer,
        connectionFactory: ConnectionFactory,
        messageConverter: Jackson2JsonMessageConverter,
    ): RabbitTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        configurer.configure(rabbitTemplate, connectionFactory)
        rabbitTemplate.messageConverter = messageConverter
        rabbitTemplate.setExchange(EXCHANGE_NAME)
        return rabbitTemplate
    }
}