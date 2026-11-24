// ============================================================
//  Práctica 11 — Pruebas White Box de TareaService
//  Método de procesamiento: crear / actualizar / eliminar
//  Cada prueba indica el camino (C1, C2...) del grafo de flujo.
// ============================================================

package com.example.taskflow.api.service;

import com.example.taskflow.api.model.Tarea;
import com.example.taskflow.api.model.Usuario;
import com.example.taskflow.api.repository.TareaRepository;
import com.example.taskflow.api.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TareaServiceTest {

    @Mock TareaRepository tareaRepository;
    @Mock UsuarioRepository usuarioRepository;
    @InjectMocks TareaService service;

    private static final String CORREO = "jorge@test.com";

    private Tarea tarea(String titulo, String estado) {
        Tarea t = new Tarea();
        t.setTitulo(titulo);
        t.setDescripcion("desc");
        t.setCategoria("Escuela");
        t.setEstado(estado);
        t.setPrioridad("alta");
        t.setFecha(LocalDate.of(2026, 11, 24));
        t.setHora(LocalTime.of(10, 0));
        return t;
    }

    // ── crear(): 2 caminos ────────────────────────────────────
    @Test
    @DisplayName("WB-crear C1: usuario existe -> se liga la tarea y visibleEnWearable=true")
    void crear_usuarioExiste() {
        Usuario u = new Usuario();
        u.setCorreo(CORREO);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(u));
        when(tareaRepository.save(any(Tarea.class))).thenAnswer(inv -> inv.getArgument(0));

        Tarea entrada = tarea("Estudiar", "pendiente");
        entrada.setVisibleEnWearable(false); // aunque venga en false, el servicio lo fuerza

        Tarea r = service.crear(entrada, CORREO);

        assertSame(u, r.getUsuario());
        assertTrue(r.isVisibleEnWearable());
        verify(tareaRepository).save(entrada);
    }

    @Test
    @DisplayName("WB-crear C2 (excepción): usuario inexistente -> RuntimeException y no se guarda")
    void crear_usuarioNoExiste() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.crear(tarea("Estudiar", "pendiente"), CORREO));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(tareaRepository, never()).save(any());
    }

    // ── actualizar(): 2 caminos ───────────────────────────────
    @Test
    @DisplayName("WB-actualizar C1: la tarea pertenece al usuario -> se copian todos los campos")
    void actualizar_existe() {
        Tarea original = tarea("Viejo", "pendiente");
        original.setId(5L);
        when(tareaRepository.findByIdAndUsuarioCorreo(5L, CORREO)).thenReturn(Optional.of(original));
        when(tareaRepository.save(any(Tarea.class))).thenAnswer(inv -> inv.getArgument(0));

        Tarea cambios = tarea("Nuevo", "finalizado");
        Optional<Tarea> r = service.actualizar(5L, cambios, CORREO);

        assertTrue(r.isPresent());
        assertEquals("Nuevo", r.get().getTitulo());
        assertEquals("finalizado", r.get().getEstado());
        assertEquals(5L, r.get().getId()); // el id no cambia
    }

    @Test
    @DisplayName("WB-actualizar C2: tarea inexistente o ajena -> Optional vacío, no se guarda")
    void actualizar_noExiste() {
        when(tareaRepository.findByIdAndUsuarioCorreo(99L, CORREO)).thenReturn(Optional.empty());

        assertTrue(service.actualizar(99L, tarea("X", "pendiente"), CORREO).isEmpty());
        verify(tareaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Defecto D-04: actualizar() con descripcion null borra la descripción guardada")
    void actualizar_parcialBorraCampos() {
        Tarea original = tarea("Tarea", "pendiente");
        original.setDescripcion("Texto importante");
        when(tareaRepository.findByIdAndUsuarioCorreo(1L, CORREO)).thenReturn(Optional.of(original));
        when(tareaRepository.save(any(Tarea.class))).thenAnswer(inv -> inv.getArgument(0));

        Tarea cambios = tarea("Tarea", "finalizado");
        cambios.setDescripcion(null); // un cliente que solo quiere cambiar el estado

        Tarea r = service.actualizar(1L, cambios, CORREO).orElseThrow();

        // Documenta el comportamiento actual: el PUT reemplaza todo (no es actualización parcial)
        assertNull(r.getDescripcion(), "El PUT sobrescribe campos no enviados");
    }

    // ── eliminar(): 2 caminos ─────────────────────────────────
    @Test
    @DisplayName("WB-eliminar C1: existe -> true y se borra")
    void eliminar_existe() {
        Tarea t = tarea("Borrar", "pendiente");
        when(tareaRepository.findByIdAndUsuarioCorreo(3L, CORREO)).thenReturn(Optional.of(t));

        assertTrue(service.eliminar(3L, CORREO));
        verify(tareaRepository).delete(t);
    }

    @Test
    @DisplayName("WB-eliminar C2: no existe -> false y no se borra nada")
    void eliminar_noExiste() {
        when(tareaRepository.findByIdAndUsuarioCorreo(3L, CORREO)).thenReturn(Optional.empty());

        assertFalse(service.eliminar(3L, CORREO));
        verify(tareaRepository, never()).delete(any());
    }

    // ── consultas: un solo camino cada una ────────────────────
    @Test
    @DisplayName("Consultas delegan con el correo y filtros correctos")
    void consultas() {
        when(tareaRepository.findByUsuarioCorreo(CORREO)).thenReturn(List.of());
        when(tareaRepository.findByUsuarioCorreoAndEstado(CORREO, "progreso")).thenReturn(List.of());
        when(tareaRepository.findByUsuarioCorreoAndVisibleEnWearable(CORREO, true)).thenReturn(List.of());
        when(tareaRepository.findByUsuarioCorreoAndEstadoAndVisibleEnWearable(CORREO, "pendiente", true))
                .thenReturn(List.of());
        when(tareaRepository.findByIdAndUsuarioCorreo(1L, CORREO)).thenReturn(Optional.empty());

        assertNotNull(service.obtenerTodas(CORREO));
        assertNotNull(service.obtenerPorEstado("progreso", CORREO));
        assertNotNull(service.obtenerParaWearable(CORREO));
        assertNotNull(service.obtenerPendientesParaWearable(CORREO));
        assertTrue(service.obtenerPorId(1L, CORREO).isEmpty());
    }
}
