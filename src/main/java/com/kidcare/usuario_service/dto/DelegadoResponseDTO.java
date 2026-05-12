package com.kidcare.usuario_service.dto;

import lombok.Data;

@Data
public class DelegadoResponseDTO {
    private Integer idUsuario;
    private Integer idDelegado;
    private String email;
    private String emailDelegado;
    private String nombreCompleto;
    private String nombreDelegado;
    private String estado;
    private String duracion;
    private String fechaExpiracion;
}
