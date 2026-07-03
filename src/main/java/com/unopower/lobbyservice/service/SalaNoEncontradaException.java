package com.unopower.lobbyservice.service;

public class SalaNoEncontradaException extends RuntimeException {
    public SalaNoEncontradaException(String codigo) {
        super("No existe una sala con el codigo '" + codigo + "'");
    }
}
