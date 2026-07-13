package com.transportista.gestionguias;

import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import com.transportista.gestionguias.dto.GuiaRequest;
import com.transportista.gestionguias.dto.GuiaResponse;
import com.transportista.gestionguias.entity.GuiaDespacho;
import com.transportista.gestionguias.messaging.GuiaDespachoPublisher;
import com.transportista.gestionguias.repository.GuiaDespachoRepository;
import com.transportista.gestionguias.service.ArchivoService;
import com.transportista.gestionguias.service.GuiaDespachoServiceImpl;
import com.transportista.gestionguias.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuiaDespachoServiceImplTest {

    @Mock
    private GuiaDespachoRepository repository;

    @Mock
    private ArchivoService archivoService;

    @Mock
    private S3Service s3Service;

    @Mock
    private GuiaDespachoPublisher publisher;

    private GuiaDespachoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GuiaDespachoServiceImpl(repository, archivoService, s3Service, publisher);
        when(repository.save(any(GuiaDespacho.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearGuiaPersisteYPublicaSinGenerarArchivo() {
        GuiaResponse response = service.crearGuia(crearRequest());

        ArgumentCaptor<GuiaDespacho> guiaCaptor = ArgumentCaptor.forClass(GuiaDespacho.class);
        ArgumentCaptor<GuiaDespachoMessage> mensajeCaptor =
                ArgumentCaptor.forClass(GuiaDespachoMessage.class);

        verify(repository).save(guiaCaptor.capture());
        verify(publisher).publicarGuia(mensajeCaptor.capture());
        verify(archivoService, never()).generarPdf(any());
        verify(s3Service, never()).subirArchivo(any(), any(), any());

        GuiaDespacho guia = guiaCaptor.getValue();
        GuiaDespachoMessage mensaje = mensajeCaptor.getValue();

        assertEquals("ENCOLADA", guia.getEstado());
        assertNotNull(guia.getMensajeId());
        assertNotNull(guia.getFechaSolicitud());
        assertNull(guia.getS3Key());
        assertEquals(guia.getMensajeId(), mensaje.getMensajeId());
        assertEquals(guia.getFechaSolicitud(), mensaje.getFechaSolicitud());
        assertEquals(guia.getNumeroGuia(), mensaje.getNumeroGuia());
        assertEquals(guia.getMensajeId(), response.getMensajeId());
        assertEquals("ENCOLADA", response.getEstado());
    }

    @Test
    void crearGuiaMarcaErrorCuandoFallaLaPublicacion() {
        doThrow(new RuntimeException("RabbitMQ no disponible"))
                .when(publisher)
                .publicarGuia(any(GuiaDespachoMessage.class));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.crearGuia(crearRequest()));

        ArgumentCaptor<GuiaDespacho> guiaCaptor = ArgumentCaptor.forClass(GuiaDespacho.class);
        verify(repository, times(2)).save(guiaCaptor.capture());
        verify(archivoService, never()).generarPdf(any());
        verify(s3Service, never()).subirArchivo(any(), any(), any());

        assertEquals("RabbitMQ no disponible", exception.getMessage());
        assertEquals("ERROR_PUBLICACION", guiaCaptor.getAllValues().get(1).getEstado());
    }

    private GuiaRequest crearRequest() {
        GuiaRequest request = new GuiaRequest();
        request.setTransportista("Transportista Uno");
        request.setCliente("Cliente Uno");
        request.setDireccionDestino("Direccion 123");
        return request;
    }
}
