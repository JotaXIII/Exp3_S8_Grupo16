package com.transportista.gestionguias.service;

import com.transportista.gestionguias.entity.GuiaDespacho;

public interface ArchivoService {

    String generarPdf(GuiaDespacho guia);

    void eliminarPdf(String rutaArchivo);
}