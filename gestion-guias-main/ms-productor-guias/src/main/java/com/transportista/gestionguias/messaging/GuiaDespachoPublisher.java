package com.transportista.gestionguias.messaging;

import com.transportista.gestionguias.config.RabbitMQConfig;
import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class GuiaDespachoPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiaDespachoPublisher.class);
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;

    public GuiaDespachoPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarGuia(GuiaDespachoMessage mensaje) {
        CorrelationData correlationData = mensaje.getMensajeId() == null
                ? new CorrelationData()
                : new CorrelationData(mensaje.getMensajeId().toString());

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
                },
                correlationData);

        esperarConfirmacion(correlationData);

        LOGGER.info(
                "Solicitud de guia confirmada: mensajeId={}, numeroGuia={}, exchange={}, routingKey={}",
                mensaje.getMensajeId(),
                mensaje.getNumeroGuia(),
                RabbitMQConfig.GUIAS_EXCHANGE,
                RabbitMQConfig.GUIAS_ROUTING_KEY);
    }

    private void esperarConfirmacion(CorrelationData correlationData) {
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!confirm.isAck()) {
                throw new AmqpException("RabbitMQ rechazo la publicacion: " + confirm.getReason());
            }

            if (correlationData.getReturned() != null) {
                throw new AmqpException("RabbitMQ retorno el mensaje sin enrutar");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AmqpException("La espera de confirmacion RabbitMQ fue interrumpida", ex);
        } catch (ExecutionException ex) {
            throw new AmqpException("No fue posible confirmar la publicacion RabbitMQ", ex.getCause());
        } catch (TimeoutException ex) {
            throw new AmqpException("RabbitMQ no confirmo la publicacion dentro del tiempo esperado", ex);
        }
    }
}
