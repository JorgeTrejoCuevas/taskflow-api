// ============================================================
//  Práctica 11 — Pruebas White Box de JwtUtil.validarToken()
//  Método de validación (backend). Caminos:
//   C1: firma y fecha válidas          -> true
//   C2: JwtException (firma/expirado)  -> false
//   C3: IllegalArgumentException (null o vacío) -> false
// ============================================================

package com.example.taskflow.api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwt = new JwtUtil();

    // Defecto D-01: la clave está escrita en el código fuente (JwtUtil.java línea 20).
    // Por eso esta prueba puede fabricar un token "legítimo" sin conocer ningún secreto del servidor.
    private static final Key CLAVE_EXPUESTA =
            Keys.hmacShaKeyFor("taskflow-secret-key-2025-muy-segura-larga".getBytes());

    @Test
    @DisplayName("C1: token recién generado es válido y conserva el correo")
    void tokenValido() {
        String t = jwt.generarToken("jorge@test.com");
        assertTrue(jwt.validarToken(t));
        assertEquals("jorge@test.com", jwt.extraerCorreo(t));
    }

    @Test
    @DisplayName("C2: token con firma alterada -> false")
    void firmaAlterada() {
        String t = jwt.generarToken("jorge@test.com");
        String alterado = t.substring(0, t.length() - 2) + (t.endsWith("A") ? "BB" : "AA");
        assertFalse(jwt.validarToken(alterado));
    }

    @Test
    @DisplayName("C2: token firmado con otra clave -> false")
    void otraClave() {
        Key otra = Keys.hmacShaKeyFor("otra-clave-distinta-de-32-bytes-minimo!!".getBytes());
        String t = Jwts.builder().setSubject("x@test.com").signWith(otra, SignatureAlgorithm.HS256).compact();
        assertFalse(jwt.validarToken(t));
    }

    @Test
    @DisplayName("C2 (valor límite): token expirado hace 1 segundo -> false")
    void tokenExpirado() {
        String t = Jwts.builder()
                .setSubject("x@test.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 10_000))
                .setExpiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(CLAVE_EXPUESTA, SignatureAlgorithm.HS256)
                .compact();
        assertFalse(jwt.validarToken(t));
    }

    @Test
    @DisplayName("C2: texto que no es JWT -> false")
    void basura() {
        assertFalse(jwt.validarToken("esto.no.es-un-token"));
    }

    @Test
    @DisplayName("C3 (valor límite): cadena vacía y null -> false sin lanzar excepción")
    void vacioYNull() {
        assertFalse(jwt.validarToken(""));
        assertFalse(jwt.validarToken(null));
    }

    @Test
    @DisplayName("Defecto D-01: con la clave del código fuente se falsifica un token aceptado")
    void claveExpuestaPermiteFalsificar() {
        String falso = Jwts.builder()
                .setSubject("cualquier-usuario@taskflow.com")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(CLAVE_EXPUESTA, SignatureAlgorithm.HS256)
                .compact();
        // Documenta el riesgo: el servidor acepta un token que nunca emitió
        assertTrue(jwt.validarToken(falso));
    }
}
