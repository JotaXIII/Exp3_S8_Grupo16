package com.transportista.gestionguias.dto;

import com.transportista.gestionguias.entity.EstadoProcesamiento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class GuiaProcesadaResponse {

    private UUID mensajeId;
    private String numeroGuia;
    private String transportista;
    private String cliente;
    private String direccionDestino;
    private LocalDate fechaEmision;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaProcesamiento;
    private String nombreArchivo;
    private String s3Key;
    private EstadoProcesamiento estado;

    public GuiaProcesadaResponse() {
    }

    public GuiaProcesadaResponse(
            UUID mensajeId,
            String numeroGuia,
            String transportista,
            String cliente,
            String direccionDestino,
            LocalDate fechaEmision,
            LocalDateTime fechaSolicitud,
            LocalDateTime fechaProcesamiento,
            String nombreArchivo,
            String s3Key,
            EstadoProcesamiento estado) {
        this.mensajeId = mensajeId;
        this.numeroGuia = numeroGuia;
        this.transportista = transportista;
        this.cliente = cliente;
        this.direccionDestino = direccionDestino;
        this.fechaEmision = fechaEmision;
        this.fechaSolicitud = fechaSolicitud;
        this.fechaProcesamiento = fechaProcesamiento;
        this.nombreArchivo = nombreArchivo;
        this.s3Key = s3Key;
        this.estado = estado;
    }

    public UUID getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(UUID mensajeId) {
        this.mensajeId = mensajeId;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public String getTransportista() {
        return transportista;
    }

    public void setTransportista(String transportista) {
        this.transportista = transportista;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getDireccionDestino() {
        return direccionDestino;
    }

    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public EstadoProcesamiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoProcesamiento estado) {
        this.estado = estado;
    }
}
