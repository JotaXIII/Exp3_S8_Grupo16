package com.transportista.gestionguias.controller;

import com.transportista.gestionguias.dto.GuiaRequest;
import com.transportista.gestionguias.dto.GuiaResponse;
import com.transportista.gestionguias.service.GuiaDespachoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/guias")
public class GuiaDespachoController {

    private final GuiaDespachoService service;

    public GuiaDespachoController(GuiaDespachoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GuiaResponse crearGuia(@Valid @RequestBody GuiaRequest request) {
        return service.crearGuia(request);
    }

    @GetMapping
    public List<GuiaResponse> listarGuias() {
        return service.listarGuias();
    }

    @GetMapping("/{id}")
    public GuiaResponse obtenerGuia(@PathVariable Long id) {
        return service.obtenerGuia(id);
    }

    @GetMapping("/historial")
    public List<GuiaResponse> buscarHistorial(
            @RequestParam(required = false) String transportista,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {

        return service.buscarHistorial(transportista, fecha);
    }

    @GetMapping("/transportista/{transportista}")
    public List<GuiaResponse> buscarPorTransportista(
            @PathVariable String transportista) {
        return service.buscarPorTransportista(transportista);
    }

    @GetMapping("/fecha/{fecha}")
    public List<GuiaResponse> buscarPorFecha(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fecha) {

        return service.buscarPorFecha(fecha);
    }

}
