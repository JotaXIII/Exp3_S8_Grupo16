package com.transportista.gestionguias.messaging;

import com.transportista.gestionguias.config.RabbitMQConfig;
import com.transportista.gestionguias.dto.GuiaEstadoMessage;
import com.transportista.gestionguias.entity.GuiaDespacho;
import com.transportista.gestionguias.repository.GuiaDespachoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class GuiaEstadoListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuiaEstadoListener.class);
    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "PROCESADA",
            "ERROR_PROCESAMIENTO",
            "ELIMINADA");

    private final GuiaDespachoRepository repository;

    public GuiaEstadoListener(GuiaDespachoRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = RabbitMQConfig.GUIAS_ESTADO_QUEUE)
    public void actualizarEstado(GuiaEstadoMessage mensaje) {
        if (mensaje == null
                || mensaje.getMensajeId() == null
                || !ESTADOS_VALIDOS.contains(mensaje.getEstado())) {
            throw new IllegalArgumentException("Evento de estado de guia invalido");
        }

        GuiaDespacho guia = repository.findByMensajeId(mensaje.getMensajeId())
                .orElse(null);
        if (guia == null) {
            LOGGER.warn(
                    "No existe solicitud para actualizar estado: mensajeId={}, numeroGuia={}",
                    mensaje.getMensajeId(),
                    mensaje.getNumeroGuia());
            return;
        }

        guia.setEstado(mensaje.getEstado());
        repository.save(guia);
        LOGGER.info(
                "Estado de solicitud actualizado: mensajeId={}, numeroGuia={}, estado={}",
                mensaje.getMensajeId(),
                mensaje.getNumeroGuia(),
                mensaje.getEstado());
    }
}
