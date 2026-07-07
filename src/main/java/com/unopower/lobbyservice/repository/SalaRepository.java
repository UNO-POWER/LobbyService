package com.unopower.lobbyservice.repository;

import com.unopower.lobbyservice.model.EstadoSala;
import com.unopower.lobbyservice.model.Sala;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SalaRepository extends JpaRepository<Sala, String> {

    Optional<Sala> findByCodigo(String codigo);

    /**
     * Variante con lock pesimista para mutaciones (unirse/iniciar): dos joins simultaneos
     * sobre la misma sala se serializan en la BD y no pueden superar el cupo maximo (HU-02).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sala s where s.codigo = :codigo")
    Optional<Sala> findByCodigoForUpdate(@Param("codigo") String codigo);

    boolean existsByCodigo(String codigo);

    /** HU-09: listado paginado de salas publicas con cupo, aun en lobby. */
    Page<Sala> findByPublicaTrueAndEstado(EstadoSala estado, Pageable pageable);
}
