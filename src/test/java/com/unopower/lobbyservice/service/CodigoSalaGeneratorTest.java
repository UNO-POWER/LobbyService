package com.unopower.lobbyservice.service;

import com.unopower.lobbyservice.repository.SalaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CodigoSalaGeneratorTest {

    @Test
    @DisplayName("HU-01: genera codigos de 6 caracteres sin simbolos ambiguos (O/0/I/1)")
    void generaCodigoDeSeisCaracteresSinAmbiguos() {
        SalaRepository repo = Mockito.mock(SalaRepository.class);
        when(repo.existsByCodigo(anyString())).thenReturn(false);
        CodigoSalaGenerator generator = new CodigoSalaGenerator(repo);

        for (int i = 0; i < 200; i++) {
            String codigo = generator.generarUnico();
            assertEquals(6, codigo.length());
            assertTrue(codigo.matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}"), "codigo invalido: " + codigo);
        }
    }

    @Test
    @DisplayName("si el codigo ya existe en BD, sigue generando hasta encontrar uno libre")
    void reintentaHastaEncontrarLibre() {
        SalaRepository repo = Mockito.mock(SalaRepository.class);
        when(repo.existsByCodigo(anyString())).thenReturn(true, true, false);
        CodigoSalaGenerator generator = new CodigoSalaGenerator(repo);

        assertNotNull(generator.generarUnico());
        verify(repo, times(3)).existsByCodigo(anyString());
    }
}
