package com.transportista.gestionguias;

import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import com.transportista.gestionguias.dto.GuiaProcesadaRequest;
import com.transportista.gestionguias.dto.GuiaProcesadaResponse;
import com.transportista.gestionguias.entity.EstadoProcesamiento;
import com.transportista.gestionguias.entity.GuiaProcesada;
import com.transportista.gestionguias.exception.GuiaProcesadaNoEncontradaException;
import com.transportista.gestionguias.messaging.GuiaEstadoPublisher;
import com.transportista.gestionguias.repository.GuiaProcesadaRepository;
import com.transportista.gestionguias.service.ArchivoService;
import com.transportista.gestionguias.service.GuiaProcesadaServiceImpl;
import com.transportista.gestionguias.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuiaProcesadaServiceImplTest {

    @Mock
    private GuiaProcesadaRepository repository;

    @Mock
    private ArchivoService archivoService;

    @Mock
    private S3Service s3Service;

    @Mock
    private GuiaEstadoPublisher estadoPublisher;

    private GuiaProcesadaServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GuiaProcesadaServiceImpl(
                repository,
                archivoService,
                s3Service,
                estadoPublisher);
    }

    @Test
    void obtieneGuiaPorNumero() {
        GuiaProcesada guia = crearGuia();
        when(repository.findByNumeroGuia(guia.getNumeroGuia())).thenReturn(Optional.of(guia));

        GuiaProcesadaResponse response = service.obtenerPorNumero(guia.getNumeroGuia());

        assertEquals(guia.getMensajeId(), response.getMensajeId());
        assertEquals(guia.getNumeroGuia(), response.getNumeroGuia());
        assertEquals(guia.getS3Key(), response.getS3Key());
    }

    @Test
    void buscaPorTransportistaYFecha() {
        GuiaProcesada guia = crearGuia();
        when(repository.findByTransportistaIgnoreCaseAndFechaEmision(
                guia.getTransportista(),
                guia.getFechaEmision()))
                .thenReturn(List.of(guia));

        List<GuiaProcesadaResponse> resultados = service.buscar(
                "  " + guia.getTransportista() + "  ",
                guia.getFechaEmision());

        assertEquals(1, resultados.size());
        assertEquals(guia.getNumeroGuia(), resultados.get(0).getNumeroGuia());
        verify(repository).findByTransportistaIgnoreCaseAndFechaEmision(
                guia.getTransportista(),
                guia.getFechaEmision());
    }

    @Test
    void descargaPdfDesdeS3() {
        GuiaProcesada guia = crearGuia();
        byte[] contenido = new byte[] {1, 2, 3};
        when(repository.findByNumeroGuia(guia.getNumeroGuia())).thenReturn(Optional.of(guia));
        when(s3Service.descargarArchivo(guia.getS3Key())).thenReturn(contenido);

        byte[] resultado = service.descargar(guia.getNumeroGuia());

        assertArrayEquals(contenido, resultado);
    }

    @Test
    void actualizaS3AntesDePersistirYEliminaTemporal() {
        GuiaProcesada guia = crearGuia();
        GuiaProcesadaRequest request = new GuiaProcesadaRequest(
                "Transportista Dos",
                "Cliente Dos",
                "Destino 456");
        File archivo = new File("GUIA-123.pdf");

        when(repository.findByNumeroGuia(guia.getNumeroGuia())).thenReturn(Optional.of(guia));
        when(archivoService.generarPdf(any(GuiaDespachoMessage.class))).thenReturn(archivo);
        when(repository.save(guia)).thenReturn(guia);

        GuiaProcesadaResponse response = service.actualizar(guia.getNumeroGuia(), request);

        InOrder orden = inOrder(s3Service, repository);
        orden.verify(s3Service).actualizarArchivo(archivo, guia.getS3Key());
        orden.verify(repository).save(guia);
        verify(archivoService).eliminarPdf(archivo);

        ArgumentCaptor<GuiaDespachoMessage> mensajeCaptor =
                ArgumentCaptor.forClass(GuiaDespachoMessage.class);
        verify(archivoService).generarPdf(mensajeCaptor.capture());
        assertEquals(request.getTransportista(), mensajeCaptor.getValue().getTransportista());
        assertEquals(request.getCliente(), response.getCliente());
        assertEquals(EstadoProcesamiento.PROCESADA, response.getEstado());
        verify(estadoPublisher).publicarActualizacion(
                guia.getMensajeId(),
                guia.getNumeroGuia(),
                request.getTransportista(),
                request.getCliente(),
                request.getDireccionDestino());
    }

    @Test
    void eliminaObjetoS3AntesDelRegistro() {
        GuiaProcesada guia = crearGuia();
        when(repository.findByNumeroGuia(guia.getNumeroGuia())).thenReturn(Optional.of(guia));

        service.eliminar(guia.getNumeroGuia());

        InOrder orden = inOrder(s3Service, estadoPublisher, repository);
        orden.verify(s3Service).eliminarArchivo(guia.getS3Key());
        orden.verify(estadoPublisher).publicarEstado(
                guia.getMensajeId(),
                guia.getNumeroGuia(),
                "ELIMINADA");
        orden.verify(repository).delete(guia);
    }

    @Test
    void informaCuandoLaGuiaNoExiste() {
        when(repository.findByNumeroGuia("GUIA-INEXISTENTE")).thenReturn(Optional.empty());

        assertThrows(
                GuiaProcesadaNoEncontradaException.class,
                () -> service.obtenerPorNumero("GUIA-INEXISTENTE"));
    }

    private GuiaProcesada crearGuia() {
        GuiaProcesada guia = new GuiaProcesada();
        guia.setMensajeId(UUID.fromString("c8c52633-2090-4bf6-a0ee-2227b61642d3"));
        guia.setNumeroGuia("GUIA-123");
        guia.setTransportista("Transportista Uno");
        guia.setCliente("Cliente Uno");
        guia.setDireccionDestino("Destino 123");
        guia.setFechaEmision(LocalDate.of(2026, 7, 13));
        guia.setFechaSolicitud(LocalDateTime.of(2026, 7, 13, 19, 30));
        guia.setFechaProcesamiento(LocalDateTime.of(2026, 7, 13, 19, 31));
        guia.setNombreArchivo("GUIA-123.pdf");
        guia.setS3Key("2026/Transportista Uno/GUIA-123.pdf");
        guia.setEstado(EstadoProcesamiento.PROCESADA);
        return guia;
    }
}
