// ============================================================
//  TaskFlow — UsuarioRepository.java
//  Acceso a la tabla de usuarios en H2.
// ============================================================

package com.example.taskflow.api.repository;

import com.example.taskflow.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
}