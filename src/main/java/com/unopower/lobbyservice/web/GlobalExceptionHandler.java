package com.unopower.lobbyservice.web;

import com.unopower.lobbyservice.service.GameServiceNoDisponibleException;
import com.unopower.lobbyservice.service.JugadoresInsuficientesException;
import com.unopower.lobbyservice.service.NoEresAnfitrionException;
import com.unopower.lobbyservice.service.SalaLlenaException;
import com.unopower.lobbyservice.service.SalaNoEncontradaException;
import com.unopower.lobbyservice.service.SalaYaIniciadaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SalaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleNoEncontrada(SalaNoEncontradaException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SalaLlenaException.class)
    public ResponseEntity<Map<String, Object>> handleLlena(SalaLlenaException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SalaYaIniciadaException.class)
    public ResponseEntity<Map<String, Object>> handleYaIniciada(SalaYaIniciadaException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NoEresAnfitrionException.class)
    public ResponseEntity<Map<String, Object>> handleNoEresAnfitrion(NoEresAnfitrionException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(JugadoresInsuficientesException.class)
    public ResponseEntity<Map<String, Object>> handleJugadoresInsuficientes(JugadoresInsuficientesException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(GameServiceNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleGameServiceNoDisponible(GameServiceNoDisponibleException ex) {
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", mensaje);
        return ResponseEntity.status(status).body(body);
    }
}
