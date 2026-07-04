package com.unopower.lobbyservice.service;

public class JugadoresInsuficientesException extends RuntimeException {
    public JugadoresInsuficientesException() {
        super("se necesitan al menos 2 jugadores para iniciar la partida");
    }
}
