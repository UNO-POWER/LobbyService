package com.unopower.lobbyservice.repository;

import com.unopower.lobbyservice.model.Sala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, String> {

    Optional<Sala> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
