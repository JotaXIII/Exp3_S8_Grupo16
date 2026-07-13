package com.transportista.gestionguias.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class GuiaDespachoMessage {

    private UUID mensajeId;
    private String numeroGuia;
    private String transportista;
    private String cliente;
    private String direccionDestino;
    private LocalDate fechaEmision;
    private LocalDateTime fechaSolicitud;

    public GuiaDespachoMessage() {
    }

    public GuiaDespachoMessage(
            UUID mensajeId,
            String numeroGuia,
            String transportista,
            String cliente,
            String direccionDestino,
            LocalDate fechaEmision,
            LocalDateTime fechaSolicitud) {
        this.mensajeId = mensajeId;
        this.numeroGuia = numeroGuia;
        this.transportista = transportista;
        this.cliente = cliente;
        this.direccionDestino = direccionDestino;
        this.fechaEmision = fechaEmision;
        this.fechaSolicitud = fechaSolicitud;
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
}
