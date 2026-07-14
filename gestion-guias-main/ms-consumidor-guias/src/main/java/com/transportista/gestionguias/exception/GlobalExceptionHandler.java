package com.transportista.gestionguias.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GuiaProcesadaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> manejarNoEncontrada(
            GuiaProcesadaNoEncontradaException ex,
            HttpServletRequest request) {
        return crearRespuesta(
                HttpStatus.NOT_FOUND,
                "El recurso solicitado no existe",
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        Object detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene datos invalidos",
                request.getRequestURI(),
                detalles);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> manejarEntradaInvalida(
            Exception ex,
            HttpServletRequest request) {
        return crearRespuesta(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene datos invalidos",
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(NoSuchKeyException.class)
    public ResponseEntity<Map<String, Object>> manejarObjetoS3NoEncontrado(
            NoSuchKeyException ex,
            HttpServletRequest request) {
        return crearRespuesta(
                HttpStatus.NOT_FOUND,
                "El archivo solicitado no existe",
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<Map<String, Object>> manejarS3(
            S3Exception ex,
            HttpServletRequest request) {
        LOGGER.error(
                "Error de S3 procesando {} {}: status={}",
                request.getMethod(), request.getRequestURI(), ex.statusCode());
        return crearRespuesta(
                HttpStatus.SERVICE_UNAVAILABLE,
                "El servicio de archivos no esta disponible",
                request.getRequestURI(),
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarErrorInterno(
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
