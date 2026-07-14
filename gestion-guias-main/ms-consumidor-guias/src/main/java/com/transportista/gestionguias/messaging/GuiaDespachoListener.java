package com.transportista.gestionguias.messaging;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.transportista.gestionguias.config.RabbitMQConfig;
import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import com.transportista.gestionguias.service.GuiaProcesamientoService;

@Component
public class GuiaDespachoListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiaDespachoListener.class);

    private final GuiaProcesamientoService procesamientoService;

    public GuiaDespachoListener(GuiaProcesamientoService procesamientoService) {
        this.procesamientoService = procesamientoService;
    }

    @RabbitListener(queues = RabbitMQConfig.GUIAS_QUEUE)
    public void recibirGuia(
            GuiaDespachoMessage mensaje,
            Message amqpMessage,
            Channel channel) throws IOException {
        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag();

        try {
            procesamientoService.procesarGuia(mensaje);
        } catch (Exception ex) {
            LOGGER.error(
                    "Error procesando guia: mensajeId={}, numeroGuia={}",
                    mensaje != null ? mensaje.getMensajeId() : null,
                    mensaje != null ? mensaje.getNumeroGuia() : null,
                    ex);
            channel.basicReject(deliveryTag, false);
            return;
        }

        channel.basicAck(deliveryTag, false);
        LOGGER.info(
                "Mensaje de guia confirmado: mensajeId={}, numeroGuia={}",
                mensaje.getMensajeId(),
                mensaje.getNumeroGuia());
    }
}
