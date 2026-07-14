package com.transportista.gestionguias;

import com.transportista.gestionguias.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void declaraRecursosDurablesYDeadLetteringCompatible() {
        DirectExchange exchange = config.guiasExchange();
        DirectExchange dlx = config.guiasDlx();
        Queue queue = config.guiasQueue();
        Queue dlq = config.guiasDlq();
        Binding queueBinding = config.guiasBinding(queue, exchange);
        Binding dlqBinding = config.guiasDlqBinding(dlq, dlx);

        assertEquals(RabbitMQConfig.GUIAS_EXCHANGE, exchange.getName());
        assertTrue(exchange.isDurable());
        assertFalse(exchange.isAutoDelete());

        assertEquals(RabbitMQConfig.GUIAS_QUEUE, queue.getName());
        assertTrue(queue.isDurable());
        assertFalse(queue.isExclusive());
        assertFalse(queue.isAutoDelete());
        assertEquals(
                RabbitMQConfig.GUIAS_DLX,
                queue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(
                RabbitMQConfig.GUIAS_DLQ_ROUTING_KEY,
                queue.getArguments().get("x-dead-letter-routing-key"));

        assertEquals(RabbitMQConfig.GUIAS_EXCHANGE, queueBinding.getExchange());
        assertEquals(RabbitMQConfig.GUIAS_QUEUE, queueBinding.getDestination());
        assertEquals(RabbitMQConfig.GUIAS_ROUTING_KEY, queueBinding.getRoutingKey());

        assertEquals(RabbitMQConfig.GUIAS_DLX, dlx.getName());
        assertTrue(dlx.isDurable());
        assertFalse(dlx.isAutoDelete());

        assertEquals(RabbitMQConfig.GUIAS_DLQ, dlq.getName());
        assertTrue(dlq.isDurable());
        assertFalse(dlq.isExclusive());
        assertFalse(dlq.isAutoDelete());

        assertEquals(RabbitMQConfig.GUIAS_DLX, dlqBinding.getExchange());
        assertEquals(RabbitMQConfig.GUIAS_DLQ, dlqBinding.getDestination());
        assertEquals(RabbitMQConfig.GUIAS_DLQ_ROUTING_KEY, dlqBinding.getRoutingKey());
    }
}
