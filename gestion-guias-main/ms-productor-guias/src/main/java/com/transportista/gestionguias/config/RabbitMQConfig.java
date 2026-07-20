package com.transportista.gestionguias.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String GUIAS_EXCHANGE = "guias.exchange";
    public static final String GUIAS_QUEUE = "guias.queue";
    public static final String GUIAS_ROUTING_KEY = "guias.routing-key";

    public static final String GUIAS_DLX = "guias.dlx";
    public static final String GUIAS_DLQ = "guias.dlq";
    public static final String GUIAS_DLQ_ROUTING_KEY = "guias.dlq.routing-key";

    public static final String GUIAS_ESTADO_EXCHANGE = "guias.estado.exchange";
    public static final String GUIAS_ESTADO_QUEUE = "guias.estado.queue";
    public static final String GUIAS_ESTADO_ROUTING_KEY = "guias.estado.routing-key";

    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(GUIAS_EXCHANGE, true, false);
    }

    @Bean
    public Queue guiasQueue() {
        return QueueBuilder.durable(GUIAS_QUEUE)
                .withArgument("x-dead-letter-exchange", GUIAS_DLX)
                .withArgument("x-dead-letter-routing-key", GUIAS_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding guiasBinding(Queue guiasQueue, DirectExchange guiasExchange) {
        return BindingBuilder.bind(guiasQueue)
                .to(guiasExchange)
                .with(GUIAS_ROUTING_KEY);
    }

    @Bean
    public DirectExchange guiasDlx() {
        return new DirectExchange(GUIAS_DLX, true, false);
    }

    @Bean
    public Queue guiasDlq() {
        return QueueBuilder.durable(GUIAS_DLQ).build();
    }

    @Bean
    public Binding guiasDlqBinding(Queue guiasDlq, DirectExchange guiasDlx) {
        return BindingBuilder.bind(guiasDlq)
                .to(guiasDlx)
                .with(GUIAS_DLQ_ROUTING_KEY);
    }

    @Bean
    public DirectExchange guiasEstadoExchange() {
        return new DirectExchange(GUIAS_ESTADO_EXCHANGE, true, false);
    }

    @Bean
    public Queue guiasEstadoQueue() {
        return QueueBuilder.durable(GUIAS_ESTADO_QUEUE).build();
    }

    @Bean
    public Binding guiasEstadoBinding(
            Queue guiasEstadoQueue,
            DirectExchange guiasEstadoExchange) {
        return BindingBuilder.bind(guiasEstadoQueue)
                .to(guiasEstadoExchange)
                .with(GUIAS_ESTADO_ROUTING_KEY);
    }
}
