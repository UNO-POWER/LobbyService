package com.unopower.lobbyservice.dto;

public record MiembroResponse(
        String userId,
        String username,
        boolean esAnfitrion
) {
}
