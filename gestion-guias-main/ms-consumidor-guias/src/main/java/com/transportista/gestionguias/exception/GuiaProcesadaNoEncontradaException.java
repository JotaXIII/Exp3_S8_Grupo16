package com.transportista.gestionguias.exception;

public class GuiaProcesadaNoEncontradaException extends RuntimeException {

    public GuiaProcesadaNoEncontradaException(String numeroGuia) {
        super("No existe una guia procesada con numero " + numeroGuia);
    }
}
