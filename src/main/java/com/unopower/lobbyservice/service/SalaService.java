package com.unopower.lobbyservice.service;

import com.unopower.lobbyservice.dto.CrearSalaRequest;
import com.unopower.lobbyservice.dto.SalaResponse;
import com.unopower.lobbyservice.model.MiembroSala;
import com.unopower.lobbyservice.model.Sala;
import com.unopower.lobbyservice.repository.SalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaService {

    private static final Logger log = LoggerFactory.getLogger(SalaService.class);
    private static final int MAX_JUGADORES = 10;

    private final SalaRepository salaRepository;
    private final CodigoSalaGenerator codigoSalaGenerator;

    public SalaService(SalaRepository salaRepository, CodigoSalaGenerator codigoSalaGenerator) {
        this.salaRepository = salaRepository;
        this.codigoSalaGenerator = codigoSalaGenerator;
    }

    @Transactional
    public SalaResponse crear(String anfitrionUserId, String anfitrionUsername, CrearSalaRequest request) {
        boolean publica = request != null && Boolean.TRUE.equals(request.publica());

        Sala sala = new Sala(codigoSalaGenerator.generarUnico(), anfitrionUserId, publica, MAX_JUGADORES);
        sala.agregarMiembro(new MiembroSala(anfitrionUserId, anfitrionUsername, true));
        sala = salaRepository.save(sala);

        log.info("sala creada codigo={} anfitrion={}", sala.getCodigo(), anfitrionUserId);
        return SalaResponse.from(sala);
    }

    @Transactional(readOnly = true)
    public SalaResponse obtener(String codigo) {
        Sala sala = salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new SalaNoEncontradaException(codigo));
        return SalaResponse.from(sala);
    }
}
