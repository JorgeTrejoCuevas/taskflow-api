// ============================================================
//  TaskFlow — JwtUtil.java
//  Genera y valida tokens JWT.
//  El token dura 24 horas y contiene el correo del usuario.
// ============================================================

package com.example.taskflow.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // Clave secreta para firmar el token — en producción va en variables de entorno
    private static final String SECRET = "taskflow-secret-key-2025-muy-segura-larga";
    private static final long EXPIRACION = 86400000L; // 24 horas en ms

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // Generar token con el correo del usuario
    public String generarToken(String correo) {
        return Jwts.builder()
                .setSubject(correo)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRACION))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extraer correo del token
    public String extraerCorreo(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // Validar si el token es válido
    public boolean validarToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}