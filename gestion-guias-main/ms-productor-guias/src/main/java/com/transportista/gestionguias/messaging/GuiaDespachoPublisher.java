package com.transportista.gestionguias.messaging;

import com.transportista.gestionguias.config.RabbitMQConfig;
import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class GuiaDespachoPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiaDespachoPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public GuiaDespachoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarGuia(GuiaDespachoMessage mensaje) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.GUIAS_EXCHANGE,
                RabbitMQConfig.GUIAS_ROUTING_KEY,
                mensaje,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);

                    if (mensaje.getMensajeId() != null) {
                        String mensajeId = mensaje.getMensajeId().toString();
                        message.getMessageProperties().setMessageId(mensajeId);
                        message.getMessageProperties().setCorrelationId(mensajeId);
                    }

                    return message;
                });

        LOGGER.info(
                "Solicitud de guia publicada: mensajeId={}, numeroGuia={}, exchange={}, routingKey={}",
                mensaje.getMensajeId(),
                mensaje.getNumeroGuia(),
                RabbitMQConfig.GUIAS_EXCHANGE,
                RabbitMQConfig.GUIAS_ROUTING_KEY);
    }
}
