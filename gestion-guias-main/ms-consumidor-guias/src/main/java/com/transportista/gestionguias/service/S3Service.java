package com.transportista.gestionguias.service;

import java.io.File;

public interface S3Service {

    String subirArchivo(File archivo, String transportista, String nombreArchivo);
}
