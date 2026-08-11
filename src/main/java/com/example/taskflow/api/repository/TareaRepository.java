// ============================================================
//  TaskFlow — TareaRepository.java
//  Ahora filtra tareas por usuario
// ============================================================

package com.example.taskflow.api.repository;

import com.example.taskflow.api.model.Tarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {

    // Buscar todas las tareas de un usuario por su correo
    List<Tarea> findByUsuarioCorreo(String correo);

    // Buscar por estado del usuario
    List<Tarea> findByUsuarioCorreoAndEstado(String correo, String estado);

    // Buscar tareas visibles en wearable del usuario
    List<Tarea> findByUsuarioCorreoAndVisibleEnWearable(String correo, boolean visible);

    // Buscar tareas pendientes para el wearable del usuario
    List<Tarea> findByUsuarioCorreoAndEstadoAndVisibleEnWearable(
        String correo, String estado, boolean visible);

    // Verificar que la tarea pertenece al usuario antes de modificar
    Optional<Tarea> findByIdAndUsuarioCorreo(Long id, String correo);
}