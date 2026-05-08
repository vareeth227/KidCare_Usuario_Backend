package com.kidcare.usuario_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// DTO que recibe los datos para registrar un nuevo usuario
@Data
public class RegistroRequestDTO {

    // Nombre completo del usuario
    @NotBlank(message = "El nombre es obligatorio")
    private String nombreCompleto;

    // Correo electrónico del usuario
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato válido")
    private String email;

    // Contraseña del usuario
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    // Teléfono opcional
    private String telefono;

    // Aceptación de términos y condiciones (obligatorio por Ley 19.628)
    private Boolean aceptaTerminos;

    // Rol solicitado: TUTOR (por defecto) o DELEGADO
    private String rolNombre;
}