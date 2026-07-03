package com.unopower.lobbyservice.service;

import com.unopower.lobbyservice.dto.SalaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Notifica al Game Service cuando cambia el roster de una sala (HU-02).
 * El Game Service es el unico dueno del WebSocket; Lobby Service permanece REST puro
 * y solo le avisa via HTTP para que difunda por /topic/sala/{codigo}/lobby.
 * Comunicacion best-effort: si Game Service no responde, no bloquea la union a la sala
 * (el cliente puede refrescar via GET /api/salas/{codigo} como fallback).
 */
@Component
public class GameServiceClient {

    private static final Logger log = LoggerFactory.getLogger(GameServiceClient.class);

    private final RestClient restClient;

    public GameServiceClient(@Value("${services.game-service.url}") String gameServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(gameServiceUrl).build();
    }

    public void notificarRosterActualizado(SalaResponse sala) {
        try {
            restClient.post()
                    .uri("/internal/salas/{codigo}/roster", sala.codigo())
                    .body(sala)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("no se pudo notificar roster a Game Service, sala={} error={}", sala.codigo(), ex.getMessage());
        }
    }
}
