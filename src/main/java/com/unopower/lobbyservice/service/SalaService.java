package com.unopower.lobbyservice.service;

import com.unopower.lobbyservice.dto.CrearSalaRequest;
import com.unopower.lobbyservice.dto.SalaResponse;
import com.unopower.lobbyservice.model.EstadoSala;
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
    private final GameServiceClient gameServiceClient;

    public SalaService(SalaRepository salaRepository, CodigoSalaGenerator codigoSalaGenerator,
                        GameServiceClient gameServiceClient) {
        this.salaRepository = salaRepository;
        this.codigoSalaGenerator = codigoSalaGenerator;
        this.gameServiceClient = gameServiceClient;
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

    @Transactional
    public SalaResponse unirse(String codigo, String userId, String username) {
        Sala sala = salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new SalaNoEncontradaException(codigo));

        if (sala.getEstado() != EstadoSala.LOBBY) {
            throw new SalaYaIniciadaException(codigo);
        }
        if (sala.getMiembros().size() >= sala.getMaxJugadores()) {
            throw new SalaLlenaException(codigo);
        }
        boolean yaEsMiembro = sala.getMiembros().stream().anyMatch(m -> m.getUserId().equals(userId));
        if (!yaEsMiembro) {
            sala.agregarMiembro(new MiembroSala(userId, username, false));
            sala = salaRepository.save(sala);
        }

        SalaResponse response = SalaResponse.from(sala);
        log.info("jugador unido codigo={} userId={} totalJugadores={}", codigo, userId, response.jugadores().size());
        gameServiceClient.notificarRosterActualizado(response);
        return response;
    }

    @Transactional(readOnly = true)
    public SalaResponse obtener(String codigo) {
        Sala sala = salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new SalaNoEncontradaException(codigo));
        return SalaResponse.from(sala);
    }

    /**
     * HU-03 (inicio de partida): solo el anfitrion, con al menos 2 jugadores. Si Game
     * Service no responde, la sala NO se marca EN_JUEGO (se propaga el error, a diferencia
     * de la notificacion best-effort del roster).
     */
    @Transactional
    public SalaResponse iniciar(String codigo, String userId) {
        Sala sala = salaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new SalaNoEncontradaException(codigo));

        if (!sala.getAnfitrionUserId().equals(userId)) {
            throw new NoEresAnfitrionException();
        }
        if (sala.getEstado() != EstadoSala.LOBBY) {
            throw new SalaYaIniciadaException(codigo);
        }
        if (sala.getMiembros().size() < 2) {
            throw new JugadoresInsuficientesException();
        }

        SalaResponse response = SalaResponse.from(sala);
        try {
            gameServiceClient.iniciarPartida(codigo, response.jugadores());
        } catch (Exception ex) {
            throw new GameServiceNoDisponibleException(ex);
        }

        sala.setEstado(EstadoSala.EN_JUEGO);
        salaRepository.save(sala);
        log.info("partida iniciada codigo={} jugadores={}", codigo, response.jugadores().size());
        return SalaResponse.from(sala);
    }
}
