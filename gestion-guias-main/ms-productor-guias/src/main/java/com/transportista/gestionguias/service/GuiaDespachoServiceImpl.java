package com.transportista.gestionguias.service;

import com.transportista.gestionguias.dto.GuiaDespachoMessage;
import com.transportista.gestionguias.dto.GuiaRequest;
import com.transportista.gestionguias.dto.GuiaResponse;
import com.transportista.gestionguias.entity.GuiaDespacho;
import com.transportista.gestionguias.exception.RecursoNoEncontradoException;
import com.transportista.gestionguias.messaging.GuiaDespachoPublisher;
import com.transportista.gestionguias.repository.GuiaDespachoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class GuiaDespachoServiceImpl implements GuiaDespachoService {

    private final GuiaDespachoRepository repository;
    private final GuiaDespachoPublisher publisher;

    public GuiaDespachoServiceImpl(
            GuiaDespachoRepository repository,
            GuiaDespachoPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Override
    public GuiaResponse crearGuia(GuiaRequest request) {
        GuiaDespacho guia = new GuiaDespacho();
        UUID mensajeId = UUID.randomUUID();
        LocalDateTime fechaSolicitud = LocalDateTime.now();
        String numeroGuia = "GUIA-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        guia.setMensajeId(mensajeId);
        guia.setFechaSolicitud(fechaSolicitud);
        guia.setNumeroGuia(numeroGuia);
        guia.setTransportista(request.getTransportista());
        guia.setCliente(request.getCliente());
        guia.setDireccionDestino(request.getDireccionDestino());
        guia.setFechaEmision(LocalDate.now());
        guia.setEstado("ENCOLADA");
        repository.save(guia);

        GuiaDespachoMessage mensaje = new GuiaDespachoMessage(
                mensajeId,
                guia.getNumeroGuia(),
                guia.getTransportista(),
                guia.getCliente(),
                guia.getDireccionDestino(),
                guia.getFechaEmision(),
                fechaSolicitud);

        try {
            publisher.publicarGuia(mensaje);
        } catch (RuntimeException ex) {
            guia.setEstado("ERROR_PUBLICACION");
            repository.save(guia);
            throw ex;
        }

        return convertir(guia);
    }

    @Override
    public List<GuiaResponse> listarGuias() {
        return repository.findAll().stream().map(this::convertir).toList();
    }

    @Override
    public GuiaResponse obtenerGuia(Long id) {
        return convertir(repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe una guia con id " + id)));
    }

    @Override
    public List<GuiaResponse> buscarPorTransportista(String transportista) {
        return repository.findByTransportistaIgnoreCase(transportista)
                .stream().map(this::convertir).toList();
    }

    @Override
    public List<GuiaResponse> buscarPorFecha(LocalDate fecha) {
        return repository.findByFechaEmision(fecha)
                .stream().map(this::convertir).toList();
    }

    @Override
    public List<GuiaResponse> buscarHistorial(String transportista, LocalDate fecha) {
        if (transportista != null && fecha != null) {
            return repository.findByTransportistaIgnoreCaseAndFechaEmision(transportista, fecha)
                    .stream().map(this::convertir).toList();
        }
        if (transportista != null) {
            return buscarPorTransportista(transportista);
        }
        if (fecha != null) {
            return buscarPorFecha(fecha);
        }
        return listarGuias();
    }

    private GuiaResponse convertir(GuiaDespacho guia) {
        return new GuiaResponse(
                guia.getId(), guia.getMensajeId(), guia.getFechaSolicitud(),
                guia.getNumeroGuia(), guia.getTransportista(), guia.getCliente(),
                guia.getDireccionDestino(), guia.getFechaEmision(), guia.getEstado());
    }
}
