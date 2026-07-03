package com.unopower.lobbyservice.service;

public class SalaYaIniciadaException extends RuntimeException {
    public SalaYaIniciadaException(String codigo) {
        super("La sala '" + codigo + "' ya inicio la partida");
    }
}
