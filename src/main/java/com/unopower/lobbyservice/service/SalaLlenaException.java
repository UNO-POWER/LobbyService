package com.unopower.lobbyservice.service;

public class SalaLlenaException extends RuntimeException {
    public SalaLlenaException(String codigo) {
        super("Sala llena");
    }
}
