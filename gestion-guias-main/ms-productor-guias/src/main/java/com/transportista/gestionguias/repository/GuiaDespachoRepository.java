package com.transportista.gestionguias.repository;

import com.transportista.gestionguias.entity.GuiaDespacho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long> {

    List<GuiaDespacho> findByTransportistaIgnoreCase(String transportista);

    List<GuiaDespacho> findByFechaEmision(LocalDate fechaEmision);

    List<GuiaDespacho> findByTransportistaIgnoreCaseAndFechaEmision(String transportista, LocalDate fechaEmision);

    boolean existsByNumeroGuia(String numeroGuia);

    Optional<GuiaDespacho> findByMensajeId(UUID mensajeId);
}
