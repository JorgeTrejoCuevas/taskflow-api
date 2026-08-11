// ============================================================
//  TaskFlow — Usuario.java
//  Entidad que representa un usuario registrado.
//  El password se guarda encriptado con BCrypt,
//  nunca en texto plano.
// ============================================================

package com.example.taskflow.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nombre;

    @NotBlank
    private String apellido;

    @NotBlank
    @Email
    @Column(unique = true)
    private String correo;

    // Guardado encriptado con BCrypt — nunca texto plano
    @NotBlank
    private String password;
}