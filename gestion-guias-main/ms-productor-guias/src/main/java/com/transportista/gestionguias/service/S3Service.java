package com.transportista.gestionguias.service;

import java.io.File;

public interface S3Service {

    String subirArchivo(
            File archivo,
            String transportista,
            String nombreArchivo);

    byte[] descargarArchivo(String s3Key);

    void actualizarArchivo(
            File archivo,
            String s3Key);

    void eliminarArchivo(String s3Key);
}