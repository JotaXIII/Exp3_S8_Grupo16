package com.transportista.gestionguias;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import com.transportista.gestionguias.entity.EstadoProcesamiento;
import com.transportista.gestionguias.entity.GuiaProcesada;
import com.transportista.gestionguias.repository.GuiaProcesadaRepository;
import com.transportista.gestionguias.service.ArchivoService;
import com.transportista.gestionguias.service.GuiaProcesamientoServiceImpl;
import com.transportista.gestionguias.service.S3Service;

@ExtendWith(MockitoExtension.class)
class GuiaProcesamientoServiceImplTest {

    @Mock
    private GuiaProcesadaRepository repository;

    @Mock
    private ArchivoService archivoService;

    @Mock
    private S3Service s3Service;

    private GuiaProcesamientoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GuiaProcesamientoServiceImpl(repository, archivoService, s3Service);
    }

    @Test
    void procesaGuiaYPersisteResultado() {
        GuiaDespachoMessage mensaje = crearMensaje();
        File archivo = new File("GUIA-123.pdf");
        String s3Key = "2026/Transportista Uno/GUIA-123.pdf";

        when(repository.existsByMensajeId(mensaje.getMensajeId())).thenReturn(false);
        when(archivoService.generarPdf(mensaje)).thenReturn(archivo);
        when(s3Service.subirArchivo(archivo, mensaje.getTransportista(), archivo.getName()))
                .thenReturn(s3Key);

        service.procesarGuia(mensaje);

        ArgumentCaptor<GuiaProcesada> guiaCaptor = ArgumentCaptor.forClass(GuiaProcesada.class);
        verify(repository).save(guiaCaptor.capture());
        verify(archivoService).eliminarPdf(archivo);

        GuiaProcesada guia = guiaCaptor.getValue();
        assertEquals(mensaje.getMensajeId(), guia.getMensajeId());
        assertEquals(mensaje.getNumeroGuia(), guia.getNumeroGuia());
        assertEquals(s3Key, guia.getS3Key());
        assertEquals(EstadoProcesamiento.PROCESADA, guia.getEstado());
        assertNotNull(guia.getFechaProcesamiento());
    }

    @Test
    void omiteMensajeYaProcesado() {
        GuiaDespachoMessage mensaje = crearMensaje();
        when(repository.existsByMensajeId(mensaje.getMensajeId())).thenReturn(true);

        service.procesarGuia(mensaje);

        verify(repository, never()).save(any(GuiaProcesada.class));
        verifyNoInteractions(archivoService, s3Service);
    }

    @Test
    void propagaFalloS3YEliminaArchivoTemporal() {
        GuiaDespachoMessage mensaje = crearMensaje();
        File archivo = new File("GUIA-123.pdf");

        when(repository.existsByMensajeId(mensaje.getMensajeId())).thenReturn(false);
        when(archivoService.generarPdf(mensaje)).thenReturn(archivo);
        when(s3Service.subirArchivo(archivo, mensaje.getTransportista(), archivo.getName()))
                .thenThrow(new IllegalStateException("S3 no disponible"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.procesarGuia(mensaje));

        assertEquals("S3 no disponible", exception.getMessage());
        verify(repository, never()).save(any(GuiaProcesada.class));
        verify(archivoService).eliminarPdf(archivo);
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
