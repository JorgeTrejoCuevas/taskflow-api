// ============================================================
//  TaskFlow — TareaService.java
//  Cada operación filtra por el usuario autenticado
// ============================================================

package com.example.taskflow.api.service;

import com.example.taskflow.api.model.Tarea;
import com.example.taskflow.api.model.Usuario;
import com.example.taskflow.api.repository.TareaRepository;
import com.example.taskflow.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TareaService {

    private final TareaRepository tareaRepository;
    private final UsuarioRepository usuarioRepository;

    public TareaService(TareaRepository tareaRepository,
                        UsuarioRepository usuarioRepository) {
        this.tareaRepository  = tareaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // ── Obtener todas las tareas del usuario ─────────────────
    public List<Tarea> obtenerTodas(String correo) {
        return tareaRepository.findByUsuarioCorreo(correo);
    }

    // ── Obtener tarea por ID (solo si pertenece al usuario) ──
    public Optional<Tarea> obtenerPorId(Long id, String correo) {
        return tareaRepository.findByIdAndUsuarioCorreo(id, correo);
    }

    // ── Crear nueva tarea ligada al usuario ──────────────────
    public Tarea crear(Tarea tarea, String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        tarea.setUsuario(usuario);
        tarea.setVisibleEnWearable(true);
        return tareaRepository.save(tarea);
    }

    // ── Actualizar tarea (solo si pertenece al usuario) ──────
    public Optional<Tarea> actualizar(Long id, Tarea datos, String correo) {
        return tareaRepository.findByIdAndUsuarioCorreo(id, correo).map(t -> {
            t.setTitulo(datos.getTitulo());
            t.setDescripcion(datos.getDescripcion());
            t.setCategoria(datos.getCategoria());
            t.setEstado(datos.getEstado());
            t.setPrioridad(datos.getPrioridad());
            t.setFecha(datos.getFecha());
            t.setHora(datos.getHora());
            t.setVisibleEnWearable(datos.isVisibleEnWearable());
            return tareaRepository.save(t);
        });
    }

    // ── Eliminar tarea (solo si pertenece al usuario) ────────
    public boolean eliminar(Long id, String correo) {
        return tareaRepository.findByIdAndUsuarioCorreo(id, correo).map(t -> {
            tareaRepository.delete(t);
            return true;
        }).orElse(false);
    }

    // ── Filtrar por estado ───────────────────────────────────
    public List<Tarea> obtenerPorEstado(String estado, String correo) {
        return tareaRepository.findByUsuarioCorreoAndEstado(correo, estado);
    }

    // ── Tareas para wearable del usuario ─────────────────────
    public List<Tarea> obtenerParaWearable(String correo) {
        return tareaRepository.findByUsuarioCorreoAndVisibleEnWearable(correo, true);
    }

    // ── Tareas pendientes para wearable ──────────────────────
    public List<Tarea> obtenerPendientesParaWearable(String correo) {
        return tareaRepository.findByUsuarioCorreoAndEstadoAndVisibleEnWearable(
            correo, "pendiente", true);
    }
}