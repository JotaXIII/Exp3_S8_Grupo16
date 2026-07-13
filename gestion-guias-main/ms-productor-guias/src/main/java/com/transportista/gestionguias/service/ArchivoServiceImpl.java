package com.transportista.gestionguias.service;

import java.io.File;
import java.io.FileOutputStream;

import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.transportista.gestionguias.config.StorageProperties;
import com.transportista.gestionguias.entity.GuiaDespacho;

@Service
public class ArchivoServiceImpl implements ArchivoService {

    private final StorageProperties storageProperties;

    public ArchivoServiceImpl(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String generarPdf(GuiaDespacho guia) {

        try {

            String storageDir = storageProperties.getStoragePath();

            File directorio = new File(storageDir);

            if (!directorio.exists()) {
                directorio.mkdirs();
            }

            String nombreArchivo = guia.getNumeroGuia() + ".pdf";

            String rutaCompleta =
                    storageDir + File.separator + nombreArchivo;

            Document document = new Document();

            PdfWriter.getInstance(
                    document,
                    new FileOutputStream(rutaCompleta));

            document.open();

            document.add(new Paragraph("GUIA DE DESPACHO - Presentacion"));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Numero: " + guia.getNumeroGuia()));
            document.add(new Paragraph("Transportista: " + guia.getTransportista()));
            document.add(new Paragraph("Cliente: " + guia.getCliente()));
            document.add(new Paragraph("Destino: " + guia.getDireccionDestino()));
            document.add(new Paragraph("Fecha: " + guia.getFechaEmision()));

            document.close();

            return rutaCompleta;

        } catch (Exception ex) {
            throw new RuntimeException("Error generando PDF", ex);
        }
    }

    @Override
    public void eliminarPdf(String rutaArchivo) {

        File archivo = new File(rutaArchivo);

        if (archivo.exists()) {
            archivo.delete();
        }
    }
}