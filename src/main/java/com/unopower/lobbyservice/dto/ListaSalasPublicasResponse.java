package com.unopower.lobbyservice.dto;

import java.util.List;

public record ListaSalasPublicasResponse(
        List<SalaPublicaResponse> salas,
        int pagina,
        int totalPaginas,
        long totalSalas
) {
}
