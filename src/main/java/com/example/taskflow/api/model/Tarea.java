// ============================================================
//  TaskFlow — Tarea.java
//  Ahora cada tarea pertenece a un usuario específico.
//  Relación ManyToOne: muchas tareas → un usuario
// ============================================================

package com.example.taskflow.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "tareas")
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}