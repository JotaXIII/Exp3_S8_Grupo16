package com.transportista.gestionguias.controller;

import com.transportista.gestionguias.dto.GuiaProcesadaRequest;
import com.transportista.gestionguias.dto.GuiaProcesadaResponse;
import com.transportista.gestionguias.service.GuiaProcesadaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/guias-procesadas")
public class GuiaProcesadaController {

    private final GuiaProcesadaService service;

    public GuiaProcesadaController(GuiaProcesadaService service) {
        this.service = service;
    }

    @GetMapping
    public List<GuiaProcesadaResponse> listarGuias() {
        return service.listarGuias();
    }

    @GetMapping("/{numeroGuia}")
    public GuiaProcesadaResponse obtenerPorNumero(@PathVariable String numeroGuia) {
        return service.obtenerPorNumero(numeroGuia);
    }

    @GetMapping("/buscar")
    public List<GuiaProcesadaResponse> buscar(
            @RequestParam(required = false) String transportista,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return service.buscar(transportista, fecha);
    }

    @GetMapping("/{numeroGuia}/descarga")
    public ResponseEntity<byte[]> descargar(@PathVariable String numeroGuia) {
        byte[] archivo = service.descargar(numeroGuia);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(numeroGuia + ".pdf")
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(archivo);
    }

    @PutMapping("/{numeroGuia}")
    public GuiaProcesadaResponse actualizar(
            @PathVariable String numeroGuia,
            @Valid @RequestBody GuiaProcesadaRequest request) {
        return service.actualizar(numeroGuia, request);
    }

    @DeleteMapping("/{numeroGuia}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String numeroGuia) {
        service.eliminar(numeroGuia);
    }
}
