package com.unopower.lobbyservice.dto;

import com.unopower.lobbyservice.model.Sala;

import java.util.List;

public record SalaResponse(
        String codigo,
        String anfitrionUserId,
        boolean publica,
        int maxJugadores,
        String estado,
        List<MiembroResponse> jugadores
) {
    public static SalaResponse from(Sala sala) {
        List<MiembroResponse> jugadores = sala.getMiembros().stream()
                .map(m -> new MiembroResponse(m.getUserId(), m.getUsername(), m.isEsAnfitrion()))
                .toList();
        return new SalaResponse(
                sala.getCodigo(),
                sala.getAnfitrionUserId(),
                sala.isPublica(),
                sala.getMaxJugadores(),
                sala.getEstado().name(),
                jugadores
        );
    }
}
