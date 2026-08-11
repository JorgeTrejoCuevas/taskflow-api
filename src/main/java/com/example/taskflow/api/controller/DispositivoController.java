// ============================================================
//  TaskFlow — DispositivoController.java
//  Endpoints para dispositivos inteligentes (wearable)
//  Actualizado para usar TareaRepository con filtro por usuario
// ============================================================

package com.example.taskflow.api.controller;

import com.example.taskflow.api.model.Tarea;
import com.example.taskflow.api.repository.TareaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dispositivos")
@CrossOrigin(origins = "http://localhost:5173")
public class DispositivoController {

    private final TareaRepository repository;

    public DispositivoController(TareaRepository repository) {
        this.repository = repository;
    }

    private String getCorreo(Authentication auth) {
        return auth.getName();
    }

    // ── GET /api/dispositivos/dom-config ─────────────────────
    @GetMapping("/dom-config")
    public ResponseEntity<Map<String, Object>> domConfig() {

        Map<String, Object> widgetTablero = new LinkedHashMap<>();
        widgetTablero.put("descripcion", "Widget principal para visualizar tareas del día");
        widgetTablero.put("selector", "#widget-tablero-tareas");
        widgetTablero.put("endpoint", "/api/tareas");
        widgetTablero.put("metodo", "GET");

        Map<String, Object> widgetWearable = new LinkedHashMap<>();
        widgetWearable.put("descripcion", "Widget de tareas pendientes visible en el smartwatch");
        widgetWearable.put("selector", "#widget-wearable-pendientes");
        widgetWearable.put("endpoint", "/api/tareas/wearable/pendientes");
        widgetWearable.put("metodo", "GET");
        widgetWearable.put("dispositivoDestino", "smartwatch");
        widgetWearable.put("resolucionObjetivo", "390x390px");

        Map<String, Object> formularioTarea = new LinkedHashMap<>();
        formularioTarea.put("descripcion", "Formulario para registrar una nueva tarea desde la web");
        formularioTarea.put("selector", "#form-nueva-tarea");
        formularioTarea.put("endpoint", "/api/tareas");
        formularioTarea.put("metodo", "POST");

        Map<String, Object> botonPwa = new LinkedHashMap<>();
        botonPwa.put("selector", "#btn-instalar-pwa");
        botonPwa.put("descripcion", "Botón para instalar TaskFlow como app en el celular");
        botonPwa.put("evento", "beforeinstallprompt");

        Map<String, Object> widgetNotificaciones = new LinkedHashMap<>();
        widgetNotificaciones.put("descripcion", "Widget de preview de notificaciones para wearable");
        widgetNotificaciones.put("selector", "#widget-preview-notificacion");
        widgetNotificaciones.put("endpoint", "/api/dispositivos/notificacion/preview/{id}");
        widgetNotificaciones.put("metodo", "GET");

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("widgetTablero",        widgetTablero);
        config.put("widgetWearable",        widgetWearable);
        config.put("formularioNuevaTarea",  formularioTarea);
        config.put("botonInstalacionPwa",   botonPwa);
        config.put("widgetNotificaciones",  widgetNotificaciones);
        config.put("flujo", "UNIDIRECCIONAL: web crea tareas → wearable las consume");

        return ResponseEntity.ok(config);
    }

    // ── GET /api/dispositivos/notificacion/preview/{id} ──────
    @GetMapping("/notificacion/preview/{id}")
    public ResponseEntity<Map<String, Object>> previewNotificacion(
            @PathVariable Long id,
            Authentication auth) {

        Optional<Tarea> opcional = repository.findByIdAndUsuarioCorreo(id, getCorreo(auth));

        if (opcional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Tarea tarea = opcional.get();

        List<String> acciones;
        if (tarea.getEstado().equals("pendiente")) {
            acciones = List.of("Marcar en progreso", "Marcar finalizado", "Posponer");
        } else if (tarea.getEstado().equals("progreso")) {
            acciones = List.of("Marcar finalizado", "Posponer");
        } else {
            acciones = List.of("Ver detalle");
        }

        Map<String, Object> preview = new LinkedHashMap<>();
        preview.put("titulo",         "Recordatorio TaskFlow");
        preview.put("mensaje",        "Tienes una tarea pendiente: " + tarea.getTitulo());
        preview.put("tarea",          tarea.getTitulo());
        preview.put("categoria",      tarea.getCategoria());
        preview.put("fecha",          tarea.getFecha().toString());
        preview.put("hora",           tarea.getHora().toString());
        preview.put("prioridad",      tarea.getPrioridad());
        preview.put("estadoActual",   tarea.getEstado().toUpperCase());
        preview.put("acciones",       acciones);
        preview.put("dispositivo",    "smartwatch");
        preview.put("resolucion",     "390x390px — área segura 300px");
        preview.put("visibleEnReloj", tarea.isVisibleEnWearable());

        return ResponseEntity.ok(preview);
    }

    // ── GET /api/dispositivos/wearable/estado ────────────────
    @GetMapping("/wearable/estado")
    public ResponseEntity<Map<String, Object>> estadoWearable(Authentication auth) {

        String correo = getCorreo(auth);
        List<Tarea> todasVisibles = repository.findByUsuarioCorreoAndVisibleEnWearable(correo, true);
        List<Tarea> pendientes    = repository.findByUsuarioCorreoAndEstadoAndVisibleEnWearable(correo, "pendiente", true);

        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("conexion",              "activa");
        estado.put("flujo",                 "web → wearable (unidireccional)");
        estado.put("totalTareasEnWearable", todasVisibles.size());
        estado.put("pendientesEnWearable",  pendientes.size());
        estado.put("endpointFlutter",       "GET /api/tareas/wearable/pendientes");

        return ResponseEntity.ok(estado);
    }
}