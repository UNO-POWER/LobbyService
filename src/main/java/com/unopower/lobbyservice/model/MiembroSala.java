package com.unopower.lobbyservice.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "miembros_sala")
public class MiembroSala {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sala_id", nullable = false)
    private Sala sala;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 40)
    private String username;

    @Column(name = "es_anfitrion", nullable = false)
    private boolean esAnfitrion;

    @Column(name = "unido_en", nullable = false)
    private Instant unidoEn = Instant.now();

    protected MiembroSala() {
        // JPA
    }

    public MiembroSala(String userId, String username, boolean esAnfitrion) {
        this.userId = userId;
        this.username = username;
        this.esAnfitrion = esAnfitrion;
    }

    public String getId() {
        return id;
    }

    public Sala getSala() {
        return sala;
    }

    public void setSala(Sala sala) {
        this.sala = sala;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public boolean isEsAnfitrion() {
        return esAnfitrion;
    }

    public Instant getUnidoEn() {
        return unidoEn;
    }
}
