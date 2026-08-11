package com.example.taskflow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String nombre;
    private String apellido;
    private String correo;
}