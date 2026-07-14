package com.transportista.gestionguias.service;

import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import com.transportista.gestionguias.entity.EstadoProcesamiento;
import com.transportista.gestionguias.entity.GuiaProcesada;
import com.transportista.gestionguias.repository.GuiaProcesadaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;

@Service
public class GuiaProcesamientoServiceImpl implements GuiaProcesamientoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiaProcesamientoServiceImpl.class);

    private final GuiaProcesadaRepository repository;
    private final ArchivoService archivoService;
    private final S3Service s3Service;

    public GuiaProcesamientoServiceImpl(
            GuiaProcesadaRepository repository,
            ArchivoService archivoService,
            S3Service s3Service) {
        this.repository = repository;
        this.archivoService = archivoService;
        this.s3Service = s3Service;
    }

    @Override
    public void procesarGuia(GuiaDespachoMessage mensaje) {
        validarMensaje(mensaje);

        if (repository.existsByMensajeId(mensaje.getMensajeId())) {
            LOGGER.info("Mensaje de guia ya procesado: mensajeId={}", mensaje.getMensajeId());
            return;
        }

        File archivo = null;
        try {
            archivo = archivoService.generarPdf(mensaje);
            String nombreArchivo = archivo.getName();
            String s3Key = s3Service.subirArchivo(
                    archivo,
                    mensaje.getTransportista(),
                    nombreArchivo);

            GuiaProcesada guia = new GuiaProcesada();
            guia.setMensajeId(mensaje.getMensajeId());
            guia.setNumeroGuia(mensaje.getNumeroGuia());
            guia.setTransportista(mensaje.getTransportista());
            guia.setCliente(mensaje.getCliente());
            guia.setDireccionDestino(mensaje.getDireccionDestino());
            guia.setFechaEmision(mensaje.getFechaEmision());
            guia.setFechaSolicitud(mensaje.getFechaSolicitud());
            guia.setFechaProcesamiento(LocalDateTime.now());
            guia.setNombreArchivo(nombreArchivo);
            guia.setS3Key(s3Key);
            guia.setEstado(EstadoProcesamiento.PROCESADA);

            repository.save(guia);

            LOGGER.info(
                    "Guia procesada: mensajeId={}, numeroGuia={}, s3Key={}",
                    mensaje.getMensajeId(),
                    mensaje.getNumeroGuia(),
                    s3Key);
        } finally {
            eliminarArchivoTemporal(archivo);
        }
    }

    private void validarMensaje(GuiaDespachoMessage mensaje) {
        if (mensaje == null) {
            throw new IllegalArgumentException("El mensaje de guia es obligatorio");
        }
        if (mensaje.getMensajeId() == null) {
            throw new IllegalArgumentException("mensajeId es obligatorio");
        }
        if (esVacio(mensaje.getNumeroGuia())) {
            throw new IllegalArgumentException("numeroGuia es obligatorio");
        }
        if (esVacio(mensaje.getTransportista())) {
            throw new IllegalArgumentException("transportista es obligatorio");
        }
        if (esVacio(mensaje.getCliente())) {
            throw new IllegalArgumentException("cliente es obligatorio");
        }
        if (esVacio(mensaje.getDireccionDestino())) {
            throw new IllegalArgumentException("direccionDestino es obligatorio");
        }
        if (mensaje.getFechaEmision() == null) {
            throw new IllegalArgumentException("fechaEmision es obligatoria");
        }
        if (mensaje.getFechaSolicitud() == null) {
            throw new IllegalArgumentException("fechaSolicitud es obligatoria");
        }
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private void eliminarArchivoTemporal(File archivo) {
        if (archivo == null) {
            return;
        }

        try {
            archivoService.eliminarPdf(archivo);
        } catch (RuntimeException ex) {
            LOGGER.warn("No se pudo eliminar el PDF temporal {}", archivo.getAbsolutePath(), ex);
        }
    }
}
