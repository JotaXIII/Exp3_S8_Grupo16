package com.transportista.gestionguias;

import com.rabbitmq.client.Channel;
import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import com.transportista.gestionguias.messaging.GuiaDespachoListener;
import com.transportista.gestionguias.service.GuiaProcesamientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GuiaDespachoListenerTest {

    private static final long DELIVERY_TAG = 42L;

    @Mock
    private GuiaProcesamientoService procesamientoService;

    @Mock
    private Channel channel;

    private GuiaDespachoListener listener;

    @BeforeEach
    void setUp() {
        listener = new GuiaDespachoListener(procesamientoService);
    }

    @Test
    void confirmaMensajeCuandoElProcesamientoTerminaCorrectamente() throws Exception {
        GuiaDespachoMessage mensaje = crearMensaje();

        listener.recibirGuia(mensaje, crearMensajeAmqp(), channel);

        verify(procesamientoService).procesarGuia(mensaje);
        verify(channel).basicAck(DELIVERY_TAG, false);
        verify(channel, never()).basicReject(DELIVERY_TAG, false);
    }

    @Test
    void rechazaSinRequeueCuandoElProcesamientoFalla() throws Exception {
        GuiaDespachoMessage mensaje = crearMensaje();
        doThrow(new IllegalStateException("Error procesando guia"))
                .when(procesamientoService)
                .procesarGuia(mensaje);

        listener.recibirGuia(mensaje, crearMensajeAmqp(), channel);

        verify(channel).basicReject(DELIVERY_TAG, false);
        verify(channel, never()).basicAck(DELIVERY_TAG, false);
    }

    private Message crearMensajeAmqp() {
        Message message = new Message(new byte[0]);
        message.getMessageProperties().setDeliveryTag(DELIVERY_TAG);
        return message;
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
}
