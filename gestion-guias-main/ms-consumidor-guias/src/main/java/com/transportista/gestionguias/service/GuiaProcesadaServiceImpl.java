package com.transportista.gestionguias.service;

import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import com.transportista.gestionguias.dto.GuiaProcesadaRequest;
import com.transportista.gestionguias.dto.GuiaProcesadaResponse;
import com.transportista.gestionguias.entity.EstadoProcesamiento;
import com.transportista.gestionguias.entity.GuiaProcesada;
import com.transportista.gestionguias.exception.GuiaProcesadaNoEncontradaException;
import com.transportista.gestionguias.messaging.GuiaEstadoPublisher;
import com.transportista.gestionguias.repository.GuiaProcesadaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class GuiaProcesadaServiceImpl implements GuiaProcesadaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiaProcesadaServiceImpl.class);

    private final GuiaProcesadaRepository repository;
    private final ArchivoService archivoService;
    private final S3Service s3Service;
    private final GuiaEstadoPublisher estadoPublisher;

    public GuiaProcesadaServiceImpl(
            GuiaProcesadaRepository repository,
            ArchivoService archivoService,
            S3Service s3Service,
            GuiaEstadoPublisher estadoPublisher) {
        this.repository = repository;
        this.archivoService = archivoService;
        this.s3Service = s3Service;
        this.estadoPublisher = estadoPublisher;
    }

    @Override
    public List<GuiaProcesadaResponse> listarGuias() {
        return repository.findAll().stream()
                .map(this::convertir)
                .toList();
    }

    @Override
    public GuiaProcesadaResponse obtenerPorNumero(String numeroGuia) {
        return convertir(buscarEntidad(numeroGuia));
    }

    @Override
    public List<GuiaProcesadaResponse> buscar(String transportista, LocalDate fecha) {
        String transportistaNormalizado = normalizarFiltro(transportista);

        List<GuiaProcesada> resultados;
        if (transportistaNormalizado != null && fecha != null) {
            resultados = repository.findByTransportistaIgnoreCaseAndFechaEmision(
                    transportistaNormalizado,
                    fecha);
        } else if (transportistaNormalizado != null) {
            resultados = repository.findByTransportistaIgnoreCase(transportistaNormalizado);
        } else if (fecha != null) {
            resultados = repository.findByFechaEmision(fecha);
        } else {
            resultados = repository.findAll();
        }

        return resultados.stream()
                .map(this::convertir)
                .toList();
    }

    @Override
    public byte[] descargar(String numeroGuia) {
        GuiaProcesada guia = buscarEntidad(numeroGuia);
        return s3Service.descargarArchivo(guia.getS3Key());
    }

    @Override
    public GuiaProcesadaResponse actualizar(String numeroGuia, GuiaProcesadaRequest request) {
        GuiaProcesada guia = buscarEntidad(numeroGuia);
        GuiaDespachoMessage mensajeActualizado = new GuiaDespachoMessage(
                guia.getMensajeId(),
                guia.getNumeroGuia(),
                request.getTransportista(),
                request.getCliente(),
                request.getDireccionDestino(),
                guia.getFechaEmision(),
                guia.getFechaSolicitud());

        File archivo = null;
        try {
            archivo = archivoService.generarPdf(mensajeActualizado);
            s3Service.actualizarArchivo(archivo, guia.getS3Key());

            guia.setTransportista(request.getTransportista());
            guia.setCliente(request.getCliente());
            guia.setDireccionDestino(request.getDireccionDestino());
            guia.setFechaProcesamiento(LocalDateTime.now());
            guia.setNombreArchivo(archivo.getName());
            guia.setEstado(EstadoProcesamiento.PROCESADA);

            GuiaProcesada actualizada = repository.save(guia);
            LOGGER.info(
                    "Guia procesada actualizada: mensajeId={}, numeroGuia={}",
                    guia.getMensajeId(),
                    guia.getNumeroGuia());
            return convertir(actualizada);
        } finally {
            eliminarArchivoTemporal(archivo);
        }
    }

    @Override
    public void eliminar(String numeroGuia) {
        GuiaProcesada guia = buscarEntidad(numeroGuia);
        s3Service.eliminarArchivo(guia.getS3Key());
        estadoPublisher.publicarEstado(
                guia.getMensajeId(),
                guia.getNumeroGuia(),
                "ELIMINADA");
        repository.delete(guia);

        LOGGER.info(
                "Guia procesada eliminada: mensajeId={}, numeroGuia={}",
                guia.getMensajeId(),
                guia.getNumeroGuia());
    }

    private GuiaProcesada buscarEntidad(String numeroGuia) {
        return repository.findByNumeroGuia(numeroGuia)
                .orElseThrow(() -> new GuiaProcesadaNoEncontradaException(numeroGuia));
    }

    private String normalizarFiltro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private void eliminarArchivoTemporal(File archivo) {
        if (archivo == null) {
            return;
        }

        try {
            archivoService.eliminarPdf(archivo);
        } catch (RuntimeException ex) {
            LOGGER.warn(
                    "No se pudo eliminar el PDF temporal: archivo={}, tipo={}",
                    archivo.getName(),
                    ex.getClass().getName());
        }
    }

    private GuiaProcesadaResponse convertir(GuiaProcesada guia) {
        return new GuiaProcesadaResponse(
                guia.getMensajeId(),
                guia.getNumeroGuia(),
                guia.getTransportista(),
                guia.getCliente(),
                guia.getDireccionDestino(),
                guia.getFechaEmision(),
                guia.getFechaSolicitud(),
                guia.getFechaProcesamiento(),
                guia.getNombreArchivo(),
                guia.getS3Key(),
                guia.getEstado());
    }
}
