package com.transportista.gestionguias;

import com.transportista.gestionguias.config.RabbitMQConfig;
import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import com.transportista.gestionguias.messaging.GuiaDespachoPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class GuiaDespachoPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publicaMensajePersistenteYEsperaAck() {
        GuiaDespachoMessage mensaje = crearMensaje();
        AtomicReference<Message> mensajeEnviado = new AtomicReference<>();

        configurarRespuesta((correlationData, message) -> {
            mensajeEnviado.set(message);
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
        });

        new GuiaDespachoPublisher(rabbitTemplate).publicarGuia(mensaje);

        Message message = mensajeEnviado.get();
        assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getDeliveryMode());
        assertEquals(mensaje.getMensajeId().toString(), message.getMessageProperties().getMessageId());
        assertEquals(mensaje.getMensajeId().toString(), message.getMessageProperties().getCorrelationId());
    }

    @Test
    void propagaNackDelBroker() {
        configurarRespuesta((correlationData, message) ->
                correlationData.getFuture().complete(new CorrelationData.Confirm(false, "exchange no disponible")));

        assertThrows(
                AmqpException.class,
                () -> new GuiaDespachoPublisher(rabbitTemplate).publicarGuia(crearMensaje()));
    }

    @Test
    void propagaMensajeRetornado() {
        configurarRespuesta((correlationData, message) -> {
            correlationData.setReturned(new ReturnedMessage(
                    message,
                    312,
                    "NO_ROUTE",
                    RabbitMQConfig.GUIAS_EXCHANGE,
                    RabbitMQConfig.GUIAS_ROUTING_KEY));
            correlationData.getFuture().complete(new CorrelationData.Confirm(true, null));
        });

        assertThrows(
                AmqpException.class,
                () -> new GuiaDespachoPublisher(rabbitTemplate).publicarGuia(crearMensaje()));
    }

    @Test
    void propagaExcepcionInmediataDeRabbitTemplate() {
        doThrow(new AmqpException("broker no disponible"))
                .when(rabbitTemplate)
                .convertAndSend(
                        eq(RabbitMQConfig.GUIAS_EXCHANGE),
                        eq(RabbitMQConfig.GUIAS_ROUTING_KEY),
                        any(),
                        any(MessagePostProcessor.class),
                        any(CorrelationData.class));

        assertThrows(
                AmqpException.class,
                () -> new GuiaDespachoPublisher(rabbitTemplate).publicarGuia(crearMensaje()));
    }

    private void configurarRespuesta(RespuestaPublicacion respuesta) {
        doAnswer(invocation -> {
            GuiaDespachoMessage mensaje = invocation.getArgument(2);
            MessagePostProcessor postProcessor = invocation.getArgument(3);
            CorrelationData correlationData = invocation.getArgument(4);
            Message message = postProcessor.postProcessMessage(new Message(new byte[0]));

            assertEquals(mensaje.getMensajeId().toString(), correlationData.getId());
            respuesta.responder(correlationData, message);
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.GUIAS_EXCHANGE),
                eq(RabbitMQConfig.GUIAS_ROUTING_KEY),
                any(),
                any(MessagePostProcessor.class),
                any(CorrelationData.class));
    }

    private GuiaDespachoMessage crearMensaje() {
        return new GuiaDespachoMessage(
                UUID.fromString("c8c52633-2090-4bf6-a0ee-2227b61642d3"),
                "GUIA-123",
                "Transportista Uno",
                "Cliente Uno",
                "Direccion 123",
                LocalDate.of(2026, 7, 13),
                LocalDateTime.of(2026, 7, 13, 19, 30));
    }

    @FunctionalInterface
    private interface RespuestaPublicacion {
        void responder(CorrelationData correlationData, Message message);
    }
}
