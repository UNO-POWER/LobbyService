package com.unopower.lobbyservice.service;

import com.unopower.lobbyservice.dto.CrearSalaRequest;
import com.unopower.lobbyservice.dto.ListaSalasPublicasResponse;
import com.unopower.lobbyservice.dto.SalaResponse;
import com.unopower.lobbyservice.model.EstadoSala;
import com.unopower.lobbyservice.model.MiembroSala;
import com.unopower.lobbyservice.model.Sala;
import com.unopower.lobbyservice.repository.SalaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SalaServiceTest {

    private static final String ANFITRION = "user-anfitrion";
    private static final String OTRO = "user-otro";

    private SalaRepository repository;
    private CodigoSalaGenerator generator;
    private GameServiceClient gameClient;
    private SalaService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(SalaRepository.class);
        generator = Mockito.mock(CodigoSalaGenerator.class);
        gameClient = Mockito.mock(GameServiceClient.class);
        TransactionTemplate tx = Mockito.mock(TransactionTemplate.class);
        // Ejecuta el callback en linea, simulando la transaccion.
        when(tx.execute(any())).thenAnswer(inv ->
                inv.<TransactionCallback<SalaResponse>>getArgument(0).doInTransaction(mock(TransactionStatus.class)));
        when(repository.save(any(Sala.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new SalaService(repository, generator, gameClient, tx);
    }

    private Sala salaConAnfitrion(boolean publica) {
        Sala sala = new Sala("ABC123", ANFITRION, publica, 10);
        sala.agregarMiembro(new MiembroSala(ANFITRION, "Ana", true));
        return sala;
    }

    // ── HU-01 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU-01: crear sala registra al anfitrion como primer miembro")
    void crearSalaConAnfitrion() {
        when(generator.generarUnico()).thenReturn("XYZ789");
        SalaResponse r = service.crear(ANFITRION, "Ana", new CrearSalaRequest(false));
        assertEquals("XYZ789", r.codigo());
        assertEquals(1, r.jugadores().size());
        assertTrue(r.jugadores().get(0).esAnfitrion());
        assertFalse(r.publica());
    }

    @Test
    @DisplayName("HU-09: crear sala publica respeta la bandera")
    void crearSalaPublica() {
        when(generator.generarUnico()).thenReturn("PUB111");
        SalaResponse r = service.crear(ANFITRION, "Ana", new CrearSalaRequest(true));
        assertTrue(r.publica());
    }

    @Test
    @DisplayName("una colision de codigo unico reintenta con codigo nuevo")
    void colisionDeCodigoReintenta() {
        AtomicInteger intentos = new AtomicInteger();
        when(generator.generarUnico()).thenReturn("DUP111", "OK2222");
        when(repository.save(any(Sala.class))).thenAnswer(inv -> {
            if (intentos.incrementAndGet() == 1) {
                throw new DataIntegrityViolationException("duplicate key");
            }
            return inv.getArgument(0);
        });

        SalaResponse r = service.crear(ANFITRION, "Ana", null);
        assertEquals("OK2222", r.codigo());
        assertEquals(2, intentos.get());
    }

    // ── HU-02 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU-02: unirse agrega al jugador y notifica el roster a Game Service")
    void unirseAgregaYNotifica() {
        Sala sala = salaConAnfitrion(false);
        when(repository.findByCodigoForUpdate("ABC123")).thenReturn(Optional.of(sala));

        SalaResponse r = service.unirse("ABC123", OTRO, "Beto");

        assertEquals(2, r.jugadores().size());
        verify(gameClient).notificarRosterActualizado(any(SalaResponse.class));
    }

    @Test
    @DisplayName("Gherkin HU-02: sala llena rechaza el ingreso")
    void unirseSalaLlena() {
        Sala sala = new Sala("FULL01", ANFITRION, false, 2);
        sala.agregarMiembro(new MiembroSala(ANFITRION, "Ana", true));
        sala.agregarMiembro(new MiembroSala("user-2", "Beto", false));
        when(repository.findByCodigoForUpdate("FULL01")).thenReturn(Optional.of(sala));

        assertThrows(SalaLlenaException.class, () -> service.unirse("FULL01", OTRO, "Carla"));
    }

    @Test
    @DisplayName("unirse a sala inexistente responde no encontrada")
    void unirseSalaInexistente() {
        when(repository.findByCodigoForUpdate("NADA99")).thenReturn(Optional.empty());
        assertThrows(SalaNoEncontradaException.class, () -> service.unirse("NADA99", OTRO, "Beto"));
    }

    @Test
    @DisplayName("unirse cuando la partida ya inicio es rechazado")
    void unirseSalaYaIniciada() {
        Sala sala = salaConAnfitrion(false);
        sala.setEstado(EstadoSala.EN_JUEGO);
        when(repository.findByCodigoForUpdate("ABC123")).thenReturn(Optional.of(sala));
        assertThrows(SalaYaIniciadaException.class, () -> service.unirse("ABC123", OTRO, "Beto"));
    }

    @Test
    @DisplayName("unirse dos veces es idempotente (no duplica al jugador)")
    void unirseIdempotente() {
        Sala sala = salaConAnfitrion(false);
        sala.agregarMiembro(new MiembroSala(OTRO, "Beto", false));
        when(repository.findByCodigoForUpdate("ABC123")).thenReturn(Optional.of(sala));

        SalaResponse r = service.unirse("ABC123", OTRO, "Beto");
        assertEquals(2, r.jugadores().size());
    }

    // ── HU-03 (iniciar) ──────────────────────────────────────────────────────

    @Test
    @DisplayName("solo el anfitrion puede iniciar la partida")
    void iniciarSoloAnfitrion() {
        Sala sala = salaConAnfitrion(false);
        sala.agregarMiembro(new MiembroSala(OTRO, "Beto", false));
        when(repository.findByCodigoForUpdate("ABC123")).thenReturn(Optional.of(sala));
        assertThrows(NoEresAnfitrionException.class, () -> service.iniciar("ABC123", OTRO));
    }

    @Test
    @DisplayName("iniciar exige al menos 2 jugadores")
    void iniciarExigeDosJugadores() {
        when(repository.findByCodigoForUpdate("ABC123")).thenReturn(Optional.of(salaConAnfitrion(false)));
        assertThrows(JugadoresInsuficientesException.class, () -> service.iniciar("ABC123", ANFITRION));
    }

    @Test
    @DisplayName("EC-04: si Game Service no responde, la sala NO queda EN_JUEGO")
    void iniciarConGameServiceCaido() {
        Sala sala = salaConAnfitrion(false);
        sala.agregarMiembro(new MiembroSala(OTRO, "Beto", false));
        when(repository.findByCodigoForUpdate("ABC123")).thenReturn(Optional.of(sala));
        doThrow(new RuntimeException("connection refused")).when(gameClient).iniciarPartida(anyString(), any());

        assertThrows(GameServiceNoDisponibleException.class, () -> service.iniciar("ABC123", ANFITRION));
        assertEquals(EstadoSala.LOBBY, sala.getEstado());
    }

    @Test
    @DisplayName("iniciar exitoso delega en Game Service y marca la sala EN_JUEGO")
    void iniciarExitoso() {
        Sala sala = salaConAnfitrion(false);
        sala.agregarMiembro(new MiembroSala(OTRO, "Beto", false));
        when(repository.findByCodigoForUpdate("ABC123")).thenReturn(Optional.of(sala));

        SalaResponse r = service.iniciar("ABC123", ANFITRION);

        verify(gameClient).iniciarPartida(eq("ABC123"), any());
        assertEquals("EN_JUEGO", r.estado());
    }

    // ── HU-09 ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("HU-09: listar publicas excluye las salas ya llenas")
    void listarPublicasExcluyeLlenas() {
        Sala conCupo = salaConAnfitrion(true);
        Sala llena = new Sala("FULL01", OTRO, true, 1);
        llena.agregarMiembro(new MiembroSala(OTRO, "Beto", true));
        when(repository.findByPublicaTrueAndEstado(eq(EstadoSala.LOBBY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(conCupo, llena)));

        ListaSalasPublicasResponse r = service.listarPublicas(0, 20);

        assertEquals(1, r.salas().size());
        assertEquals("ABC123", r.salas().get(0).codigo());
        assertEquals("Ana", r.salas().get(0).anfitrionUsername());
    }

    @Test
    @DisplayName("HU-09: el tamano de pagina se acota para proteger el servicio")
    void listarPublicasAcotaTamano() {
        when(repository.findByPublicaTrueAndEstado(eq(EstadoSala.LOBBY), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listarPublicas(-5, 9999);

        verify(repository).findByPublicaTrueAndEstado(eq(EstadoSala.LOBBY), argThat((Pageable p) ->
                p.getPageNumber() == 0 && p.getPageSize() == 50));
    }
}
