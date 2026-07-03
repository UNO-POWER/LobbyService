package com.unopower.lobbyservice.service;

import com.unopower.lobbyservice.repository.SalaRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CodigoSalaGenerator {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sin O/0/I/1 para evitar confusion
    private static final int LONGITUD = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SalaRepository salaRepository;

    public CodigoSalaGenerator(SalaRepository salaRepository) {
        this.salaRepository = salaRepository;
    }

    public String generarUnico() {
        String codigo;
        do {
            codigo = generar();
        } while (salaRepository.existsByCodigo(codigo));
        return codigo;
    }

    private String generar() {
        StringBuilder sb = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }
}
