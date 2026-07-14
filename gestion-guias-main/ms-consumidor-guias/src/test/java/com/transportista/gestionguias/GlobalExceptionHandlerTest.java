package com.transportista.gestionguias;

import com.transportista.gestionguias.exception.GlobalExceptionHandler;
import com.transportista.gestionguias.exception.GuiaProcesadaNoEncontradaException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void responde404SinExponerElDetalleInterno() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/guias-procesadas/GUIA-99");

        ResponseEntity<Map<String, Object>> response = handler.manejarNoEncontrada(
                new GuiaProcesadaNoEncontradaException("GUIA-99"),
                request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("El recurso solicitado no existe", body(response).get("mensaje"));
        assertFalse(body(response).toString().contains("No existe una guia procesada"));
    }

    @Test
    void responde500SinExponerDetallesTecnicos() {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/api/guias-procesadas/GUIA-99/descarga");
        String detalleSensible = "AccessKey=secreta; bucket=interno";

        ResponseEntity<Map<String, Object>> response = handler.manejarErrorInterno(
                new IllegalStateException(detalleSensible),
                request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Ocurrio un error interno en el servidor", body(response).get("mensaje"));
        assertEquals("/api/guias-procesadas/GUIA-99/descarga", body(response).get("ruta"));
        assertFalse(body(response).toString().contains(detalleSensible));
    }

    private Map<String, Object> body(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        return body;
    }
}
