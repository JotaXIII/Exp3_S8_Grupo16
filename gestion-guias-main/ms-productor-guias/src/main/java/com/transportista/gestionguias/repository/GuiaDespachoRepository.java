package com.transportista.gestionguias.repository;

import com.transportista.gestionguias.entity.GuiaDespacho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long> {

    List<GuiaDespacho> findByTransportistaIgnoreCase(String transportista);

    List<GuiaDespacho> findByFechaEmision(LocalDate fechaEmision);

    List<GuiaDespacho> findByTransportistaIgnoreCaseAndFechaEmision(String transportista, LocalDate fechaEmision);

    boolean existsByNumeroGuia(String numeroGuia);
}