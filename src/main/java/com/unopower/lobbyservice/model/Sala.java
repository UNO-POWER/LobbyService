package com.unopower.lobbyservice.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "salas", uniqueConstraints = @UniqueConstraint(columnNames = "codigo"))
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 6)
    private String codigo;

    @Column(name = "anfitrion_user_id", nullable = false)
    private String anfitrionUserId;

    @Column(nullable = false)
    private boolean publica = false;

    @Column(name = "max_jugadores", nullable = false)
    private int maxJugadores = 10;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSala estado = EstadoSala.LOBBY;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn = Instant.now();

    @OneToMany(mappedBy = "sala", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<MiembroSala> miembros = new ArrayList<>();

    protected Sala() {
        // JPA
    }

    public Sala(String codigo, String anfitrionUserId, boolean publica, int maxJugadores) {
        this.codigo = codigo;
        this.anfitrionUserId = anfitrionUserId;
        this.publica = publica;
        this.maxJugadores = maxJugadores;
    }

    public String getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getAnfitrionUserId() {
        return anfitrionUserId;
    }

    public boolean isPublica() {
        return publica;
    }

    public int getMaxJugadores() {
        return maxJugadores;
    }

    public EstadoSala getEstado() {
        return estado;
    }

    public void setEstado(EstadoSala estado) {
        this.estado = estado;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public List<MiembroSala> getMiembros() {
        return miembros;
    }

    public void agregarMiembro(MiembroSala miembro) {
        miembro.setSala(this);
        this.miembros.add(miembro);
    }
}
