package com.unopower.lobbyservice.service;

import com.unopower.lobbyservice.dto.MiembroResponse;
import com.unopower.lobbyservice.dto.SalaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

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

    public GameServiceClient(@Value("${services.game-service.url}") String gameServiceUrl,
                              @Value("${internal.api-key}") String internalApiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(gameServiceUrl)
                .defaultHeader("X-Internal-Key", internalApiKey)
                .build();
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

    /**
     * A diferencia de notificarRosterActualizado, esta llamada NO es best-effort: si Game
     * Service no responde, la partida no existe realmente y el error debe propagarse para
     * que Lobby Service no marque la sala como EN_JUEGO sin partida real detras.
     */
    public void iniciarPartida(String codigo, List<MiembroResponse> jugadores) {
        restClient.post()
                .uri("/internal/partidas/{codigo}/iniciar", codigo)
                .body(new IniciarPartidaRequest(jugadores))
                .retrieve()
                .toBodilessEntity();
    }

    private record IniciarPartidaRequest(List<MiembroResponse> jugadores) {
    }
}
