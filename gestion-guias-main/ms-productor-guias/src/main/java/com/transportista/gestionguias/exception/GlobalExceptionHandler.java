package com.transportista.gestionguias.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidaciones(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Object detalles = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene datos invalidos",
                request.getRequestURI(),
                detalles);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrado(
            RecursoNoEncontradoException ex,
            HttpServletRequest request) {
        return crearRespuesta(
                HttpStatus.NOT_FOUND,
                "El recurso solicitado no existe",
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> manejarJsonInvalido(
            Exception ex,
            HttpServletRequest request) {
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es valido",
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, Object>> manejarRabbitMQ(
            AmqpException ex,
            HttpServletRequest request) {
        LOGGER.error(
                "RabbitMQ no disponible procesando {} {}: tipo={}",
                request.getMethod(), request.getRequestURI(), ex.getClass().getName());
        return crearRespuesta(
                HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de mensajeria no esta disponible",
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarException(
            Exception ex,
            HttpServletRequest request) {
        LOGGER.error(
                "Error interno procesando {} {}: tipo={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getClass().getName());
        return crearRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error interno en el servidor",
                request.getRequestURI(),
                null);
    }

    private ResponseEntity<Map<String, Object>> crearRespuesta(
            HttpStatus estado,
            String mensaje,
            String ruta,
            Object detalles) {
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("fecha", LocalDateTime.now());
        respuesta.put("estado", estado.value());
        respuesta.put("mensaje", mensaje);
        respuesta.put("ruta", ruta);
        if (detalles != null) {
            respuesta.put("detalles", detalles);
        }
        return ResponseEntity.status(estado).body(respuesta);
    }
}
