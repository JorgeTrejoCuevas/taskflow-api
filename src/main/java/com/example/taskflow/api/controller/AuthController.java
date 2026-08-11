// ============================================================
//  TaskFlow — AuthController.java
//  Endpoints: registro, login, perfil, cambiar-password
// ============================================================

package com.example.taskflow.api.controller;

import com.example.taskflow.api.dto.AuthResponse;
import com.example.taskflow.api.dto.LoginRequest;
import com.example.taskflow.api.dto.RegisterRequest;
import com.example.taskflow.api.model.Usuario;
import com.example.taskflow.api.repository.UsuarioRepository;
import com.example.taskflow.api.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder   = passwordEncoder;
        this.jwtUtil           = jwtUtil;
    }

    // ── POST /api/auth/registro ──────────────────────────────
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegisterRequest req) {

        if (usuarioRepository.existsByCorreo(req.getCorreo())) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "El correo ya está registrado"));
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(req.getNombre());
        usuario.setApellido(req.getApellido());
        usuario.setCorreo(req.getCorreo());
        usuario.setPassword(passwordEncoder.encode(req.getPassword()));
        usuarioRepository.save(usuario);

        String token = jwtUtil.generarToken(usuario.getCorreo());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(new AuthResponse(
                token,
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo()
            ));
    }

    // ── POST /api/auth/login ─────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreo(req.getCorreo());

        if (usuarioOpt.isEmpty() ||
            !passwordEncoder.matches(req.getPassword(), usuarioOpt.get().getPassword())) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Correo o contraseña incorrectos"));
        }

        Usuario usuario = usuarioOpt.get();
        String token = jwtUtil.generarToken(usuario.getCorreo());

        return ResponseEntity.ok(new AuthResponse(
            token,
            usuario.getNombre(),
            usuario.getApellido(),
            usuario.getCorreo()
        ));
    }

    // ── PUT /api/auth/perfil ─────────────────────────────────
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(
            @RequestBody RegisterRequest req,
            Authentication auth) {

        String correo = auth.getName();
        Optional<Usuario> opt = usuarioRepository.findByCorreo(correo);

        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Usuario usuario = opt.get();
        usuario.setNombre(req.getNombre());
        usuario.setApellido(req.getApellido());
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of(
            "nombre",   usuario.getNombre(),
            "apellido", usuario.getApellido(),
            "correo",   usuario.getCorreo()
        ));
    }

    // ── PUT /api/auth/cambiar-password ───────────────────────
    @PutMapping("/cambiar-password")
    public ResponseEntity<?> cambiarPassword(
            @RequestBody Map<String, String> req,
            Authentication auth) {

        String correo = auth.getName();
        Optional<Usuario> opt = usuarioRepository.findByCorreo(correo);

        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Usuario usuario = opt.get();

        if (!passwordEncoder.matches(req.get("passwordActual"), usuario.getPassword())) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Contraseña actual incorrecta"));
        }

        usuario.setPassword(passwordEncoder.encode(req.get("passwordNueva")));
        usuarioRepository.save(usuario);

        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente"));
    }
}