package com.kidcare.usuario_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompletarRegistroDTO {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    // Opcional para usuarios existentes (Ruta 2: vincular)
    private String nombreCompleto;

    // Opcional para usuarios existentes (Ruta 2: vincular)
    private String password;
}
