package com.unopower.lobbyservice.service;

public class GameServiceNoDisponibleException extends RuntimeException {
    public GameServiceNoDisponibleException(Throwable causa) {
        super("no se pudo iniciar la partida: Game Service no disponible", causa);
    }
}
