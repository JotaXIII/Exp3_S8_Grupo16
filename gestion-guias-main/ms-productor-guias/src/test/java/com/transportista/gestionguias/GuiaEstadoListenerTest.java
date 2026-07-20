package com.transportista.gestionguias;

import com.transportista.gestionguias.dto.GuiaEstadoMessage;
import com.transportista.gestionguias.entity.GuiaDespacho;
import com.transportista.gestionguias.messaging.GuiaEstadoListener;
import com.transportista.gestionguias.repository.GuiaDespachoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuiaEstadoListenerTest {

    @Mock
    private GuiaDespachoRepository repository;

    private GuiaEstadoListener listener;

    @BeforeEach
    void setUp() {
        listener = new GuiaEstadoListener(repository);
    }

    @Test
    void actualizaSolicitudProcesadaPorMensajeId() {
        UUID mensajeId = UUID.randomUUID();
        GuiaDespacho guia = new GuiaDespacho();
        guia.setMensajeId(mensajeId);
        guia.setEstado("ENCOLADA");
        when(repository.findByMensajeId(mensajeId)).thenReturn(Optional.of(guia));

        listener.actualizarEstado(new GuiaEstadoMessage(
                mensajeId,
                "GUIA-123",
                "PROCESADA",
                LocalDateTime.now()));

        assertEquals("PROCESADA", guia.getEstado());
        verify(repository).save(guia);
    }

    @Test
    void ignoraEventoCuandoLaSolicitudYaNoExiste() {
        UUID mensajeId = UUID.randomUUID();
        when(repository.findByMensajeId(mensajeId)).thenReturn(Optional.empty());

        listener.actualizarEstado(new GuiaEstadoMessage(
                mensajeId,
                "GUIA-123",
                "ELIMINADA",
                LocalDateTime.now()));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaEstadoDesconocido() {
        GuiaEstadoMessage mensaje = new GuiaEstadoMessage(
                UUID.randomUUID(),
                "GUIA-123",
                "DESCONOCIDO",
                LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () -> listener.actualizarEstado(mensaje));
    }
}
