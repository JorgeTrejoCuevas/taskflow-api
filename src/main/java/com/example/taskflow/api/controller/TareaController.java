// ============================================================
//  TaskFlow — TareaController.java
//  Extrae el correo del usuario desde el token JWT
//  usando SecurityContextHolder para saber quién hace la petición
// ============================================================

package com.example.taskflow.api.controller;

import com.example.taskflow.api.model.Tarea;
import com.example.taskflow.api.service.TareaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tareas")
@CrossOrigin(origins = "http://localhost:5173")
public class TareaController {

    private final TareaService service;

    public TareaController(TareaService service) {
        this.service = service;
    }

    // Helper para obtener el correo del usuario autenticado
    private String getCorreo(Authentication auth) {
        return auth.getName();
    }

    // ── GET /api/tareas ──────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Tarea>> obtenerTodas(Authentication auth) {
        return ResponseEntity.ok(service.obtenerTodas(getCorreo(auth)));
    }

    // ── GET /api/tareas/{id} ─────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Tarea> obtenerPorId(@PathVariable Long id, Authentication auth) {
        return service.obtenerPorId(id, getCorreo(auth))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── POST /api/tareas ─────────────────────────────────────
    @PostMapping
    public ResponseEntity<Tarea> crear(@Valid @RequestBody Tarea tarea, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.crear(tarea, getCorreo(auth)));
    }

    // ── PUT /api/tareas/{id} ─────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<Tarea> actualizar(@PathVariable Long id,
                                             @Valid @RequestBody Tarea tarea,
                                             Authentication auth) {
        return service.actualizar(id, tarea, getCorreo(auth))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── DELETE /api/tareas/{id} ──────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        return service.eliminar(id, getCorreo(auth))
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // ── GET /api/tareas/estado/{estado} ─────────────────────
    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<Tarea>> porEstado(@PathVariable String estado,
                                                   Authentication auth) {
        return ResponseEntity.ok(service.obtenerPorEstado(estado, getCorreo(auth)));
    }

    // ── GET /api/tareas/wearable ─────────────────────────────
    @GetMapping("/wearable")
    public ResponseEntity<List<Tarea>> paraWearable(Authentication auth) {
        return ResponseEntity.ok(service.obtenerParaWearable(getCorreo(auth)));
    }

    // ── GET /api/tareas/wearable/pendientes ──────────────────
    @GetMapping("/wearable/pendientes")
    public ResponseEntity<List<Tarea>> pendientesWearable(Authentication auth) {
        return ResponseEntity.ok(service.obtenerPendientesParaWearable(getCorreo(auth)));
    }
}