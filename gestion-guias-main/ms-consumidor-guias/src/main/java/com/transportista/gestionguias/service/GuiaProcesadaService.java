package com.transportista.gestionguias.service;

import com.transportista.gestionguias.dto.GuiaProcesadaRequest;
import com.transportista.gestionguias.dto.GuiaProcesadaResponse;

import java.time.LocalDate;
import java.util.List;

public interface GuiaProcesadaService {

    List<GuiaProcesadaResponse> listarGuias();

    GuiaProcesadaResponse obtenerPorNumero(String numeroGuia);

    List<GuiaProcesadaResponse> buscar(String transportista, LocalDate fecha);

    byte[] descargar(String numeroGuia);

    GuiaProcesadaResponse actualizar(String numeroGuia, GuiaProcesadaRequest request);

    void eliminar(String numeroGuia);
}
