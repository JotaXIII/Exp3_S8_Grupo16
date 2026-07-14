package com.transportista.gestionguias.service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.transportista.gestionguias.config.StorageProperties;
import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class ArchivoServiceImpl implements ArchivoService {

    private final StorageProperties storageProperties;

    public ArchivoServiceImpl(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public File generarPdf(GuiaDespachoMessage mensaje) {
        Path directorio = Path.of(storageProperties.getStoragePath());
        Path rutaArchivo = directorio.resolve(mensaje.getNumeroGuia() + ".pdf");

        try {
            Files.createDirectories(directorio);

            Document document = new Document();
            try (OutputStream outputStream = Files.newOutputStream(rutaArchivo)) {
                PdfWriter.getInstance(document, outputStream);
                document.open();
                document.add(new Paragraph("GUIA DE DESPACHO"));
                document.add(new Paragraph(" "));
                document.add(new Paragraph("Numero: " + mensaje.getNumeroGuia()));
                document.add(new Paragraph("Transportista: " + mensaje.getTransportista()));
                document.add(new Paragraph("Cliente: " + mensaje.getCliente()));
                document.add(new Paragraph("Destino: " + mensaje.getDireccionDestino()));
                document.add(new Paragraph("Fecha: " + mensaje.getFechaEmision()));
                document.close();
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }

            return rutaArchivo.toFile();
        } catch (Exception ex) {
            throw new IllegalStateException("Error generando PDF para la guia " + mensaje.getNumeroGuia(), ex);
        }
    }

    @Override
    public void eliminarPdf(File archivo) {
        try {
            Files.deleteIfExists(archivo.toPath());
        } catch (Exception ex) {
            throw new IllegalStateException("Error eliminando el PDF temporal " + archivo.getName(), ex);
        }
    }
}
