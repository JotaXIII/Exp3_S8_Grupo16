package com.transportista.gestionguias.repository;

import com.transportista.gestionguias.entity.GuiaProcesada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GuiaProcesadaRepository extends JpaRepository<GuiaProcesada, Long> {

    boolean existsByMensajeId(UUID mensajeId);
}
