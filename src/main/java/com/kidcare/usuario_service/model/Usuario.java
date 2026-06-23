package com.kidcare.usuario_service.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa a un usuario del sistema.
 *
 * <p>Incluye usuarios con rol TUTOR (padre/tutor principal), DELEGADO (apoderado)
 * y ADMIN. El campo {@code tokenRecuperacion} y {@code fechaExpiracionToken}
 * se usan exclusivamente durante el flujo de recuperación de contraseña por correo.
 *
 * <p>Hibernate genera la tabla {@code USUARIO} automáticamente según los campos
 * ({@code ddl-auto=update}).
 */
@Data
@Entity
@Table(name = "USUARIO")
public class Usuario {

    // Identificador único del usuario
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    // Rol asignado al usuario
    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    // Nombre completo del usuario
    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    // Correo electrónico usado para login, debe ser único
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    // Contraseña encriptada con BCrypt
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Teléfono de contacto opcional
    @Column(name = "telefono")
    private String telefono;

    // Indica si la cuenta está habilitada
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // Indica si la cuenta fue eliminada por el administrador (soft-delete)
    @Column(name = "eliminado", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean eliminado = false;

    // Fecha de registro en el sistema
    @Column(name = "fechaCreacion", nullable = false)
    private LocalDate fechaCreacion;

    // Token para recuperación de contraseña
    @Column(name = "tokenRecuperacion")
    private String tokenRecuperacion;

    // Fecha y hora de expiración del token de recuperación (30 minutos)
    @Column(name = "fechaExpiracionToken")
    private LocalDateTime fechaExpiracionToken;
}