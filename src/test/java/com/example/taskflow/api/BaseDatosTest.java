// ============================================================
//  Práctica 11 — Fase 4: White Box de la base de datos
//  Corre contra H2 en memoria (perfil dev) con las mismas
//  entidades y restricciones que Hibernate crea en PostgreSQL.
//  Las consultas EXPLAIN de PostgreSQL (Railway) están en
//  practica11/consultas_postgres.sql
// ============================================================

package com.example.taskflow.api;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import com.example.taskflow.api.model.Tarea;
import com.example.taskflow.api.model.Usuario;
import com.example.taskflow.api.repository.TareaRepository;
import com.example.taskflow.api.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BaseDatosTest {

    @Autowired UsuarioRepository usuarios;
    @Autowired TareaRepository tareas;
    @Autowired JdbcTemplate jdbc;

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setNombre("Prueba");
        u.setApellido("BD");
        u.setCorreo("bd_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
        u.setPassword("$2a$10$hashDePrueba");
        return usuarios.save(u);
    }

    private Tarea tarea(Usuario u, String titulo) {
        Tarea t = new Tarea();
        t.setTitulo(titulo);
        t.setDescripcion("desc");
        t.setCategoria("Escuela");
        t.setEstado("pendiente");
        t.setPrioridad("media");
        t.setFecha(LocalDate.of(2026, 11, 30));
        t.setHora(LocalTime.of(9, 0));
        t.setUsuario(u);
        return t;
    }

    @Test
    @DisplayName("DB-01: inserción válida genera id y persiste")
    void insercionValida() {
        Tarea t = tareas.save(tarea(usuario(), "Insertar"));
        assertNotNull(t.getId());
        assertTrue(tareas.findById(t.getId()).isPresent());
    }

    @Test
    @DisplayName("DB-02: inserción con título nulo se rechaza (NOT NULL / @NotBlank)")
    void insercionInvalida() {
        Tarea t = tarea(usuario(), null);
        assertThrows(Exception.class, () -> tareas.saveAndFlush(t));
    }

    @Test
    @DisplayName("DB-02: correo duplicado viola UNIQUE")
    void correoUnico() {
        Usuario u = usuario();
        Usuario copia = new Usuario();
        copia.setNombre("X");
        copia.setApellido("Y");
        copia.setCorreo(u.getCorreo());
        copia.setPassword("x");
        assertThrows(Exception.class, () -> usuarios.saveAndFlush(copia));
    }

    @Test
    @DisplayName("DB-02: borrar un usuario con tareas viola la llave foránea")
    void llaveForanea() {
        Usuario u = usuario();
        tareas.save(tarea(u, "Depende del usuario"));
        assertThrows(Exception.class, () -> {
            usuarios.delete(u);
            usuarios.flush();
        });
    }

    @Test
    @DisplayName("Defecto DB-02: una tarea sin usuario (usuario_id NULL) no debería poder guardarse")
    void tareaHuerfana() {
        Tarea t = tarea(null, "Tarea sin dueño");
        assertThrows(Exception.class, () -> tareas.saveAndFlush(t),
                "usuario_id permite NULL: se pueden crear tareas que nadie puede ver");
    }

    @Test
    @DisplayName("DB-03: consulta por id existente e inexistente; filtro por usuario")
    void consultas() {
        Usuario a = usuario();
        Usuario b = usuario();
        Tarea t = tareas.save(tarea(a, "De A"));
        assertTrue(tareas.findByIdAndUsuarioCorreo(t.getId(), a.getCorreo()).isPresent());
        assertTrue(tareas.findByIdAndUsuarioCorreo(t.getId(), b.getCorreo()).isEmpty());
        assertTrue(tareas.findById(999_999L).isEmpty());
    }

    @Test
    @DisplayName("DB-04 / DB-05: actualizar y eliminar, verificando persistencia después")
    void actualizarEliminar() {
        Tarea t = tareas.save(tarea(usuario(), "Antes"));
        t.setTitulo("Después");
        tareas.save(t);
        assertEquals("Después", tareas.findById(t.getId()).orElseThrow().getTitulo());

        tareas.deleteById(t.getId());
        assertTrue(tareas.findById(t.getId()).isEmpty());
    }

    @Test
    @DisplayName("DB-06: dos ediciones concurrentes -> la segunda se rechaza (409) y no se pierde ningún cambio (@Version)")
    void actualizacionPerdida() {
        Tarea t = tareas.save(tarea(usuario(), "Original"));

        // Dos clientes (web y reloj) leen la misma versión
        Tarea web = tareas.findById(t.getId()).orElseThrow();
        Tarea reloj = tareas.findById(t.getId()).orElseThrow();

        web.setTitulo("Título editado en la web");
        reloj.setEstado("finalizado");

        tareas.save(web);
        // Antes de @Version esta línea sobrescribía el título en silencio (last-write-wins)
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> tareas.save(reloj),
                "El segundo guardado trae una versión vieja y debe rechazarse");

        Tarea fin = tareas.findById(t.getId()).orElseThrow();
        System.out.println("[DB-06] título final = " + fin.getTitulo() + " | estado final = " + fin.getEstado());
        assertEquals("Título editado en la web", fin.getTitulo(), "El cambio de la web se conserva");
        assertEquals(1L, fin.getVersion());
    }

    @Test
    @DisplayName("Medición: tiempo de 3 consultas con 500 tareas y plan de ejecución en H2")
    void tiemposYPlan() {
        Usuario u = usuario();
        List<Tarea> lote = new ArrayList<>();
        for (int i = 0; i < 500; i++) lote.add(tarea(u, "Tarea " + i));
        tareas.saveAll(lote);

        medir("findByUsuarioCorreo", () -> tareas.findByUsuarioCorreo(u.getCorreo()).size());
        medir("findByUsuarioCorreoAndEstado", () -> tareas.findByUsuarioCorreoAndEstado(u.getCorreo(), "pendiente").size());
        Long id = lote.get(250).getId();
        medir("findByIdAndUsuarioCorreo", () -> tareas.findByIdAndUsuarioCorreo(id, u.getCorreo()).isPresent() ? 1 : 0);

        jdbc.queryForList("EXPLAIN SELECT t.* FROM tareas t JOIN usuarios u ON u.id = t.usuario_id WHERE u.correo = ?",
                        u.getCorreo())
                .forEach(f -> System.out.println("[EXPLAIN H2] " + f.values()));
        jdbc.queryForList("SELECT INDEX_NAME, TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS "
                        + "WHERE TABLE_NAME IN ('TAREAS','USUARIOS')")
                .forEach(f -> System.out.println("[INDICES H2] " + f.values()));
    }

    private void medir(String nombre, java.util.function.IntSupplier consulta) {
        consulta.getAsInt(); // calentamiento
        long[] tiempos = new long[3];
        int filas = 0;
        for (int i = 0; i < 3; i++) {
            long t0 = System.nanoTime();
            filas = consulta.getAsInt();
            tiempos[i] = (System.nanoTime() - t0) / 1000;
        }
        System.out.printf("[DB-TIEMPO] %-30s filas=%d  tiempos(us)=%d, %d, %d%n",
                nombre, filas, tiempos[0], tiempos[1], tiempos[2]);
    }
}
