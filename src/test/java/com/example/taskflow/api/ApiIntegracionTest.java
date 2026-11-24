// ============================================================
//  Práctica 11 — Fase 3: White Box de los endpoints
//  Levanta la API real (perfil dev, H2 en memoria) en un puerto
//  aleatorio y le hace peticiones HTTP reales.
//  Las pruebas marcadas "Defecto" esperan el comportamiento
//  CORRECTO; si fallan, el fallo es la evidencia del defecto.
// ============================================================

package com.example.taskflow.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegracionTest {

    @Value("${local.server.port}")
    int port;

    private final HttpClient client = HttpClient.newHttpClient();

    record Resp(int code, String body, long ms) {}

    private Resp http(String metodo, String ruta, String json, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta))
                .method(metodo, json == null ? HttpRequest.BodyPublishers.noBody()
                                             : HttpRequest.BodyPublishers.ofString(json));
        if (json != null) b.header("Content-Type", "application/json");
        if (token != null) b.header("Authorization", "Bearer " + token);
        long t0 = System.nanoTime();
        HttpResponse<String> r = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf("[HTTP] %-6s %-28s -> %d (%d ms, %d bytes)%n",
                metodo, ruta, r.statusCode(), ms, r.body().length());
        return new Resp(r.statusCode(), r.body(), ms);
    }

    private static String campo(String json, String nombre) {
        Matcher m = Pattern.compile("\"" + nombre + "\"\\s*:\\s*\"?([^\",}]+)").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private String correoNuevo() {
        return "p11_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com";
    }

    private String registrar(String correo) throws Exception {
        Resp r = http("POST", "/api/auth/registro",
                "{\"nombre\":\"Prueba\",\"apellido\":\"P11\",\"correo\":\"" + correo + "\",\"password\":\"Clave123!\"}", null);
        assertEquals(201, r.code());
        return campo(r.body(), "token");
    }

    private static String tareaJson(String titulo) {
        return "{\"titulo\":\"" + titulo + "\",\"descripcion\":\"Prueba P11\",\"categoria\":\"Escuela\","
             + "\"estado\":\"pendiente\",\"prioridad\":\"alta\",\"fecha\":\"2026-11-30\",\"hora\":\"10:00:00\"}";
    }

    // ── Autenticación ────────────────────────────────────────
    @Test
    @DisplayName("WEB-01: registro válido -> 201 con token y sin contraseña en la respuesta")
    void registroValido() throws Exception {
        Resp r = http("POST", "/api/auth/registro",
                "{\"nombre\":\"Ana\",\"apellido\":\"Lopez\",\"correo\":\"" + correoNuevo() + "\",\"password\":\"Clave123!\"}", null);
        assertEquals(201, r.code());
        assertNotNull(campo(r.body(), "token"));
        assertFalse(r.body().toLowerCase().contains("password"));
    }

    @Test
    @DisplayName("WEB-02: correo repetido -> 409 Conflict")
    void registroDuplicado() throws Exception {
        String c = correoNuevo();
        registrar(c);
        Resp r = http("POST", "/api/auth/registro",
                "{\"nombre\":\"A\",\"apellido\":\"B\",\"correo\":\"" + c + "\",\"password\":\"Clave123!\"}", null);
        assertEquals(409, r.code());
    }

    @Test
    @DisplayName("Defecto WEB-03: registro con cuerpo vacío debe ser 400, no 500")
    void registroVacio() throws Exception {
        Resp r = http("POST", "/api/auth/registro", "{}", null);
        assertEquals(400, r.code(), "El backend no valida RegisterRequest (sin @Valid)");
    }

    @Test
    @DisplayName("Defecto WEB-03: correo con formato inválido debe ser 400")
    void registroCorreoInvalido() throws Exception {
        Resp r = http("POST", "/api/auth/registro",
                "{\"nombre\":\"A\",\"apellido\":\"B\",\"correo\":\"no-es-correo\",\"password\":\"x\"}", null);
        assertEquals(400, r.code());
    }

    @Test
    @DisplayName("WEB-04: login correcto 200, contraseña mala 401, usuario inexistente 401")
    void login() throws Exception {
        String c = correoNuevo();
        registrar(c);
        assertEquals(200, http("POST", "/api/auth/login",
                "{\"correo\":\"" + c + "\",\"password\":\"Clave123!\"}", null).code());
        assertEquals(401, http("POST", "/api/auth/login",
                "{\"correo\":\"" + c + "\",\"password\":\"mala\"}", null).code());
        assertEquals(401, http("POST", "/api/auth/login",
                "{\"correo\":\"nadie@test.com\",\"password\":\"x\"}", null).code());
    }

    @Test
    @DisplayName("Defecto WEB-04: sin token debe ser 401 (no autenticado), no 403")
    void sinToken() throws Exception {
        assertEquals(401, http("GET", "/api/tareas", null, null).code());
    }

    @Test
    @DisplayName("Defecto WEB-04: token inválido debe ser 401")
    void tokenInvalido() throws Exception {
        assertEquals(401, http("GET", "/api/tareas", null, "token.falso.123").code());
    }

    // ── Tareas: CRUD y validación ────────────────────────────
    @Test
    @DisplayName("WEB-01/DB: crear 201 -> consultar 200 -> eliminar 204 -> consultar 404")
    void cicloCompleto() throws Exception {
        String token = registrar(correoNuevo());

        Resp creada = http("POST", "/api/tareas", tareaJson("Tarea de ciclo"), token);
        assertEquals(201, creada.code());
        String id = campo(creada.body(), "id");

        assertEquals(200, http("GET", "/api/tareas/" + id, null, token).code());
        assertEquals(204, http("DELETE", "/api/tareas/" + id, null, token).code());
        assertEquals(404, http("GET", "/api/tareas/" + id, null, token).code());
        assertEquals(404, http("DELETE", "/api/tareas/" + id, null, token).code());
    }

    @Test
    @DisplayName("Defecto WEB-SEC: la respuesta de una tarea no debe incluir datos del usuario ni su contraseña")
    void tareaNoExponeUsuario() throws Exception {
        String token = registrar(correoNuevo());
        Resp creada = http("POST", "/api/tareas", tareaJson("Sin datos sensibles"), token);
        assertEquals(201, creada.code());
        assertFalse(creada.body().contains("password"),
                "La entidad Usuario (con hash BCrypt) se serializa dentro de la tarea");
    }

    @Test
    @DisplayName("WEB-03 valores límite del título: 2 chars -> 400, 3 chars -> 201, vacío -> 400")
    void limitesTitulo() throws Exception {
        String token = registrar(correoNuevo());
        assertEquals(400, http("POST", "/api/tareas", tareaJson("ab"), token).code());
        assertEquals(201, http("POST", "/api/tareas", tareaJson("abc"), token).code());
        assertEquals(400, http("POST", "/api/tareas", tareaJson(""), token).code());
        assertEquals(400, http("POST", "/api/tareas", tareaJson("x".repeat(151)), token).code());
        assertEquals(201, http("POST", "/api/tareas", tareaJson("x".repeat(150)), token).code());
    }

    @Test
    @DisplayName("WEB-03: JSON mal formado -> 400")
    void jsonMalFormado() throws Exception {
        String token = registrar(correoNuevo());
        assertEquals(400, http("POST", "/api/tareas", "{titulo:", token).code());
    }

    @Test
    @DisplayName("WEB-04 autorización: un usuario no puede ver ni borrar la tarea de otro")
    void tareaAjena() throws Exception {
        String tokenA = registrar(correoNuevo());
        String tokenB = registrar(correoNuevo());
        String id = campo(http("POST", "/api/tareas", tareaJson("Privada de A"), tokenA).body(), "id");

        assertEquals(404, http("GET", "/api/tareas/" + id, null, tokenB).code());
        assertEquals(404, http("DELETE", "/api/tareas/" + id, null, tokenB).code());
        assertEquals(200, http("GET", "/api/tareas/" + id, null, tokenA).code()); // sigue existiendo
    }

    @Test
    @DisplayName("WEB-02: editar con una versión vieja -> 409 Conflict y no se pierde el cambio anterior")
    void conflictoDeVersion() throws Exception {
        String token = registrar(correoNuevo());
        String id = campo(http("POST", "/api/tareas", tareaJson("Original"), token).body(), "id");
        String v0 = "{\"version\":0," + tareaJson("Editada en la web").substring(1);
        String v0b = "{\"version\":0," + tareaJson("Editada en el reloj").substring(1);

        assertEquals(200, http("PUT", "/api/tareas/" + id, v0, token).code());
        assertEquals(409, http("PUT", "/api/tareas/" + id, v0b, token).code());
        assertTrue(http("GET", "/api/tareas/" + id, null, token).body().contains("Editada en la web"));
    }

    @Test
    @DisplayName("WEB-02: actualizar tarea inexistente -> 404")
    void actualizarInexistente() throws Exception {
        String token = registrar(correoNuevo());
        assertEquals(404, http("PUT", "/api/tareas/999999", tareaJson("No existe"), token).code());
    }

    @Test
    @DisplayName("WEB-06: 10 solicitudes iguales devuelven mismo código y mismo tamaño")
    void consistencia() throws Exception {
        String token = registrar(correoNuevo());
        http("POST", "/api/tareas", tareaJson("Consistencia"), token);
        Resp primera = http("GET", "/api/tareas", null, token);
        for (int i = 0; i < 9; i++) {
            Resp r = http("GET", "/api/tareas", null, token);
            assertEquals(primera.code(), r.code());
            assertEquals(primera.body().length(), r.body().length());
        }
    }
}
