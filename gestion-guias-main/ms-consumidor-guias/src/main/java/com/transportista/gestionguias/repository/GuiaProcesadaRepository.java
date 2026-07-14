package com.transportista.gestionguias.repository;

import com.transportista.gestionguias.entity.GuiaProcesada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuiaProcesadaRepository extends JpaRepository<GuiaProcesada, Long> {

    boolean existsByMensajeId(UUID mensajeId);

    Optional<GuiaProcesada> findByNumeroGuia(String numeroGuia);

    List<GuiaProcesada> findByTransportistaIgnoreCase(String transportista);

    List<GuiaProcesada> findByFechaEmision(LocalDate fechaEmision);

    List<GuiaProcesada> findByTransportistaIgnoreCaseAndFechaEmision(
            String transportista,
            LocalDate fechaEmision);
}
