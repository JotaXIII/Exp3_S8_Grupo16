package com.transportista.gestionguias.service;

import com.transportista.gestionguias.dto.GuiaRequest;
import com.transportista.gestionguias.dto.GuiaResponse;

import java.time.LocalDate;
import java.util.List;

public interface GuiaDespachoService {

    GuiaResponse crearGuia(GuiaRequest request);

    List<GuiaResponse> listarGuias();

    GuiaResponse obtenerGuia(Long id);

    List<GuiaResponse> buscarPorTransportista(String transportista);

    List<GuiaResponse> buscarPorFecha(LocalDate fecha);

    List<GuiaResponse> buscarHistorial(String transportista, LocalDate fecha);
}
