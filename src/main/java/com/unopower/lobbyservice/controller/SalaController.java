package com.unopower.lobbyservice.controller;

import com.unopower.lobbyservice.dto.CrearSalaRequest;
import com.unopower.lobbyservice.dto.SalaResponse;
import com.unopower.lobbyservice.service.SalaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salas")
public class SalaController {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @PostMapping
    public ResponseEntity<SalaResponse> crear(@RequestBody(required = false) CrearSalaRequest request,
                                               HttpServletRequest httpRequest) {
        String userId = requireHeader(httpRequest, USER_ID_HEADER);
        String username = requireHeader(httpRequest, USERNAME_HEADER);
        SalaResponse sala = salaService.crear(userId, username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sala);
    }

    @PostMapping("/{codigo}/unirse")
    public ResponseEntity<SalaResponse> unirse(@PathVariable String codigo, HttpServletRequest httpRequest) {
        String userId = requireHeader(httpRequest, USER_ID_HEADER);
        String username = requireHeader(httpRequest, USERNAME_HEADER);
        return ResponseEntity.ok(salaService.unirse(codigo.toUpperCase(), userId, username));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<SalaResponse> obtener(@PathVariable String codigo) {
        return ResponseEntity.ok(salaService.obtener(codigo.toUpperCase()));
    }

    @PostMapping("/{codigo}/iniciar")
    public ResponseEntity<SalaResponse> iniciar(@PathVariable String codigo, HttpServletRequest httpRequest) {
        String userId = requireHeader(httpRequest, USER_ID_HEADER);
        return ResponseEntity.ok(salaService.iniciar(codigo.toUpperCase(), userId));
    }

    private String requireHeader(HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Encabezado requerido ausente: " + header
                    + " (deberia venir del API Gateway tras validar el JWT)");
        }
        return value;
    }
}
