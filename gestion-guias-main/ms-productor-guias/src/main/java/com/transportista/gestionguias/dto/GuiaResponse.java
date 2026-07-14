package com.transportista.gestionguias.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class GuiaResponse {

    private Long id;
    private UUID mensajeId;
    private LocalDateTime fechaSolicitud;
    private String numeroGuia;
    private String transportista;
    private String cliente;
    private String direccionDestino;
    private LocalDate fechaEmision;
    private String estado;

    public GuiaResponse() {
    }

    public GuiaResponse(Long id, String numeroGuia, String transportista, String cliente,
                        String direccionDestino, LocalDate fechaEmision, String estado) {
        this(id, null, null, numeroGuia, transportista, cliente,
                direccionDestino, fechaEmision, estado);
    }

    public GuiaResponse(Long id, UUID mensajeId, LocalDateTime fechaSolicitud,
                        String numeroGuia, String transportista, String cliente,
                        String direccionDestino, LocalDate fechaEmision, String estado) {
        this.id = id;
        this.mensajeId = mensajeId;
        this.fechaSolicitud = fechaSolicitud;
        this.numeroGuia = numeroGuia;
        this.transportista = transportista;
        this.cliente = cliente;
        this.direccionDestino = direccionDestino;
        this.fechaEmision = fechaEmision;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public UUID getMensajeId() { return mensajeId; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public String getNumeroGuia() { return numeroGuia; }
    public String getTransportista() { return transportista; }
    public String getCliente() { return cliente; }
    public String getDireccionDestino() { return direccionDestino; }
    public LocalDate getFechaEmision() { return fechaEmision; }
    public String getEstado() { return estado; }
}
