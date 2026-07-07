package com.unopower.lobbyservice.dto;

import com.unopower.lobbyservice.model.Sala;

/** HU-09: resumen de una sala publica visible en el matchmaking (sin datos sensibles). */
public record SalaPublicaResponse(
        String codigo,
        String anfitrionUsername,
        int jugadores,
        int maxJugadores
) {
    public static SalaPublicaResponse from(Sala sala) {
        String anfitrion = sala.getMiembros().stream()
                .filter(m -> m.isEsAnfitrion())
                .map(m -> m.getUsername())
                .findFirst()
                .orElse("?");
        return new SalaPublicaResponse(sala.getCodigo(), anfitrion, sala.getMiembros().size(), sala.getMaxJugadores());
    }
}
