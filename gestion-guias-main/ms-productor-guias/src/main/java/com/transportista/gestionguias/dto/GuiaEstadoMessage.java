package com.transportista.gestionguias.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class GuiaEstadoMessage {

    private UUID mensajeId;
    private String numeroGuia;
    private String estado;
    private LocalDateTime fechaActualizacion;

    public GuiaEstadoMessage() {
    }

    public GuiaEstadoMessage(
            UUID mensajeId,
            String numeroGuia,
            String estado,
            LocalDateTime fechaActualizacion) {
        this.mensajeId = mensajeId;
        this.numeroGuia = numeroGuia;
        this.estado = estado;
        this.fechaActualizacion = fechaActualizacion;
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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
