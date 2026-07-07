package com.unopower.lobbyservice.service;

import com.unopower.lobbyservice.dto.CrearSalaRequest;
import com.unopower.lobbyservice.dto.ListaSalasPublicasResponse;
import com.unopower.lobbyservice.dto.SalaPublicaResponse;
import com.unopower.lobbyservice.dto.SalaResponse;
import com.unopower.lobbyservice.model.EstadoSala;
import com.unopower.lobbyservice.model.MiembroSala;
import com.unopower.lobbyservice.model.Sala;
import com.unopower.lobbyservice.repository.SalaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class SalaService {

    private static final Logger log = LoggerFactory.getLogger(SalaService.class);
    private static final int MAX_JUGADORES = 10;
    private static final int MAX_INTENTOS_CODIGO = 3;
    private static final int TAMANO_PAGINA_MAXIMO = 50;

    private final SalaRepository salaRepository;
    private final CodigoSalaGenerator codigoSalaGenerator;
    private final GameServiceClient gameServiceClient;
    private final TransactionTemplate transactionTemplate;

    public SalaService(SalaRepository salaRepository, CodigoSalaGenerator codigoSalaGenerator,
                        GameServiceClient gameServiceClient, TransactionTemplate transactionTemplate) {
        this.salaRepository = salaRepository;
        this.codigoSalaGenerator = codigoSalaGenerator;
        this.gameServiceClient = gameServiceClient;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * HU-01. El retry cubre la ventana entre "verificar codigo libre" y "guardar": si dos
     * salas generan el mismo codigo a la vez, la restriccion UNIQUE detiene a la segunda y
     * aqui se reintenta con un codigo nuevo en una transaccion nueva.
     */
    public SalaResponse crear(String anfitrionUserId, String anfitrionUsername, CrearSalaRequest request) {
        boolean publica = request != null && Boolean.TRUE.equals(request.publica());
        for (int intento = 1; ; intento++) {
            try {
                return transactionTemplate.execute(tx -> {
                    Sala sala = new Sala(codigoSalaGenerator.generarUnico(), anfitrionUserId, publica, MAX_JUGADORES);
                    sala.agregarMiembro(new MiembroSala(anfitrionUserId, anfitrionUsername, true));
                    Sala guardada = salaRepository.save(sala);
                    log.info("sala creada codigo={} publica={} anfitrion={}", guardada.getCodigo(), publica, anfitrionUserId);
                    return SalaResponse.from(guardada);
                });
            } catch (DataIntegrityViolationException ex) {
                if (intento >= MAX_INTENTOS_CODIGO) {
                    throw ex;
                }
                log.warn("colision de codigo de sala (intento {}), reintentando", intento);
            }
        }
    }

    /** HU-02. Lock pesimista: dos joins simultaneos no pueden superar el cupo maximo. */
    @Transactional
    public SalaResponse unirse(String codigo, String userId, String username) {
        Sala sala = salaRepository.findByCodigoForUpdate(codigo)
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

    /** HU-09: salas publicas con cupo, paginadas (DoD: respuesta paginada &lt;300ms). */
    @Transactional(readOnly = true)
    public ListaSalasPublicasResponse listarPublicas(int pagina, int tamano) {
        int tamanoSeguro = Math.min(Math.max(tamano, 1), TAMANO_PAGINA_MAXIMO);
        Page<Sala> resultado = salaRepository.findByPublicaTrueAndEstado(
                EstadoSala.LOBBY,
                PageRequest.of(Math.max(pagina, 0), tamanoSeguro, Sort.by(Sort.Direction.DESC, "creadaEn")));
        return new ListaSalasPublicasResponse(
                resultado.getContent().stream()
                        .filter(s -> s.getMiembros().size() < s.getMaxJugadores())
                        .map(SalaPublicaResponse::from)
                        .toList(),
                resultado.getNumber(),
                resultado.getTotalPages(),
                resultado.getTotalElements());
    }

    /**
     * HU-03 (inicio de partida): solo el anfitrion, con al menos 2 jugadores. Si Game
     * Service no responde, la sala NO se marca EN_JUEGO (se propaga el error, a diferencia
     * de la notificacion best-effort del roster).
     */
    @Transactional
    public SalaResponse iniciar(String codigo, String userId) {
        Sala sala = salaRepository.findByCodigoForUpdate(codigo)
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
