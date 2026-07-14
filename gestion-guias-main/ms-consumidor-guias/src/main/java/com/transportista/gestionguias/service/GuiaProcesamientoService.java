package com.transportista.gestionguias.service;

import com.transportista.gestionguias.dto.GuiaDespachoMessage;

public interface GuiaProcesamientoService {

    void procesarGuia(GuiaDespachoMessage mensaje);
}
