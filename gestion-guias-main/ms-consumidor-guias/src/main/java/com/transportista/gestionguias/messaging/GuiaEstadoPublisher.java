package com.transportista.gestionguias.messaging;

import com.transportista.gestionguias.config.RabbitMQConfig;
import com.transportista.gestionguias.dto.GuiaEstadoMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class GuiaEstadoPublisher {

    private final RabbitTemplate rabbitTemplate;

    public GuiaEstadoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarEstado(
            UUID mensajeId,
            String numeroGuia,
            String estado) {
        GuiaEstadoMessage mensaje = new GuiaEstadoMessage(
                mensajeId,
                numeroGuia,
                estado,
                LocalDateTime.now());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.GUIAS_ESTADO_EXCHANGE,
                RabbitMQConfig.GUIAS_ESTADO_ROUTING_KEY,
                mensaje);
    }
}
