package com.transportista.gestionguias.dto;

import java.time.LocalDateTime;

public class ErrorResponse {

    private LocalDateTime fecha;
    private int estado;
    private String mensaje;
    private String error;

    public ErrorResponse() {
    }

    public ErrorResponse(LocalDateTime fecha, int estado, String mensaje, String error) {
        this.fecha = fecha;
        this.estado = estado;
        this.mensaje = mensaje;
        this.error = error;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public int getEstado() {
        return estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getError() {
        return error;
    }
}