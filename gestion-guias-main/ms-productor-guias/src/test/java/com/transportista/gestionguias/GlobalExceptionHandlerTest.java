package com.transportista.gestionguias;

import com.transportista.gestionguias.exception.GlobalExceptionHandler;
import com.transportista.gestionguias.exception.RecursoNoEncontradoException;
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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/guias/99");

        ResponseEntity<Map<String, Object>> response = handler.manejarNoEncontrado(
                new RecursoNoEncontradoException("registro interno 99 no encontrado"),
                request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("El recurso solicitado no existe", body(response).get("mensaje"));
        assertFalse(body(response).toString().contains("registro interno"));
    }

    @Test
    void responde500SinExponerDetallesTecnicos() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/guias");
        String detalleSensible = "AccessKey=secreta; rabbit-host=interno";

        ResponseEntity<Map<String, Object>> response = handler.manejarException(
                new IllegalStateException(detalleSensible),
                request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Ocurrio un error interno en el servidor", body(response).get("mensaje"));
        assertEquals("/api/guias", body(response).get("ruta"));
        assertFalse(body(response).toString().contains(detalleSensible));
    }

    private Map<String, Object> body(ResponseEntity<Map<String, Object>> response) {
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        return body;
    }
}
