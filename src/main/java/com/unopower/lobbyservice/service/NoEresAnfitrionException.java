package com.unopower.lobbyservice.service;

public class NoEresAnfitrionException extends RuntimeException {
    public NoEresAnfitrionException() {
        super("solo el anfitrion puede iniciar la partida");
    }
}
