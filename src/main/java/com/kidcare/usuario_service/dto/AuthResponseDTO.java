package com.kidcare.usuario_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO de respuesta para las operaciones de login y registro.
 *
 * <p>Retorna el token JWT listo para usar, el email del usuario y su rol,
 * de forma que el cliente Android pueda persistirlos en SessionManager.
 */
@Data
@AllArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String email;
    private String rol;
    private Integer idUsuario;
    private String nombreCompleto;
}