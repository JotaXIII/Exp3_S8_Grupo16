package com.transportista.gestionguias.service;

import com.transportista.gestionguias.dto.GuiaDespachoMessage;

import java.io.File;

public interface ArchivoService {

    File generarPdf(GuiaDespachoMessage mensaje);

    void eliminarPdf(File archivo);
}
