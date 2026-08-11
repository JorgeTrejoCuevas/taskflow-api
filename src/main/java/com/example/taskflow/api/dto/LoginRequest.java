// ============================================================
//  TaskFlow — DTOs de autenticación
//  Archivo: dto/LoginRequest.java
// ============================================================

package com.example.taskflow.api.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String correo;
    private String password;
}