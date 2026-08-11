// ============================================================
//  TaskFlow — ApiApplication.java
//  Sin datos precargados — cada usuario crea sus propias tareas
// ============================================================

package com.example.taskflow.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}