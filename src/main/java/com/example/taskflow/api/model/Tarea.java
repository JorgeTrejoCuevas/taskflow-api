// ============================================================
//  TaskFlow — Tarea.java
//  Ahora cada tarea pertenece a un usuario específico.
//  Relación ManyToOne: muchas tareas → un usuario
// ============================================================

package com.example.taskflow.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "tareas", indexes = {
        // Práctica 12: índice para las consultas por usuario (antes: Seq Scan)
        @Index(name = "idx_tareas_usuario", columnList = "usuario_id"),
        @Index(name = "idx_tareas_usuario_estado", columnList = "usuario_id, estado")
})
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Prácticas 13-14: control de concurrencia optimista.
    // Cada cambio incrementa la versión; si alguien guarda con una versión vieja -> 409.
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private Long version = 0L;

    @NotBlank(message = "El título no puede estar vacío")
    @Size(min = 3, max = 150)
    @Column(nullable = false)
    private String titulo;

    @Size(max = 500)
    private String descripcion;

    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotBlank(message = "La prioridad es obligatoria")
    private String prioridad;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    @Column(name = "visible_wearable")
    private boolean visibleEnWearable = true;

    // Relación con el usuario dueño de la tarea
    // FetchType.LAZY = no carga el usuario a menos que se pida
    // Práctica 12: @JsonIgnore evita enviar el usuario (y su hash) en cada tarea
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
}