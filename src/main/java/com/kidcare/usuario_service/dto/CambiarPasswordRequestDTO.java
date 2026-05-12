package com.kidcare.usuario_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CambiarPasswordRequestDTO {

    @NotBlank
    private String passwordActual;

    @NotBlank
    private String passwordNueva;
}
