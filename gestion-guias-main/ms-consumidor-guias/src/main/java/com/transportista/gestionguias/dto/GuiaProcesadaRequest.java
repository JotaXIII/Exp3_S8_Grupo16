package com.transportista.gestionguias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GuiaProcesadaRequest {

    @NotBlank(message = "El transportista es obligatorio")
    @Size(max = 100, message = "El transportista no puede superar los 100 caracteres")
    private String transportista;

    @NotBlank(message = "El cliente es obligatorio")
    @Size(max = 100, message = "El cliente no puede superar los 100 caracteres")
    private String cliente;

    @NotBlank(message = "La direccion de destino es obligatoria")
    @Size(max = 200, message = "La direccion de destino no puede superar los 200 caracteres")
    private String direccionDestino;

    public GuiaProcesadaRequest() {
    }

    public GuiaProcesadaRequest(String transportista, String cliente, String direccionDestino) {
        this.transportista = transportista;
        this.cliente = cliente;
        this.direccionDestino = direccionDestino;
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
}
