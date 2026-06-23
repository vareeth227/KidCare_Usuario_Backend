package com.kidcare.usuario_service.service;

import com.kidcare.usuario_service.dto.*;
import com.kidcare.usuario_service.model.Rol;
import com.kidcare.usuario_service.model.Usuario;
import com.kidcare.usuario_service.repository.RolRepository;
import com.kidcare.usuario_service.repository.UsuarioRepository;
import com.kidcare.usuario_service.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Servicio de autenticación y gestión de cuentas de usuario.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Registro de nuevos usuarios (solo TUTOR — el rol DELEGADO lo asigna el tutor)</li>
 *   <li>Autenticación y generación de token JWT</li>
 *   <li>Solicitud y restablecimiento de contraseña via email</li>
 * </ul>
 *
 * <p>Política de contraseñas: mínimo 8 caracteres, al menos una mayúscula y un símbolo especial.
 */
@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    /**
     * Valida que la contraseña cumpla la política de seguridad del sistema.
     * Mínimo 8 caracteres, al menos una mayúscula y un símbolo especial.
     */
    private void validarPassword(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("La contraseña debe tener al menos 8 caracteres");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new RuntimeException("La contraseña debe contener al menos una letra mayúscula");
        }
        if (password.chars().allMatch(c -> Character.isLetterOrDigit(c))) {
            throw new RuntimeException("La contraseña debe contener al menos un símbolo especial (!@#$%...)");
        }
    }

    /**
     * Registra un nuevo usuario en el sistema con rol TUTOR.
     *
     * <p>El registro público solo permite crear cuentas TUTOR.
     * El rol DELEGADO es asignado por el tutor mediante {@code /api/delegados/vincular}.
     * Valida la política de contraseña antes de guardar.
     *
     * @param dto nombre completo, email, contraseña, teléfono (opcional), aceptaTerminos
     * @return token JWT + email + nombre del rol asignado
     * @throws RuntimeException si el email ya existe, no acepta términos o la contraseña no cumple la política
     */
    public AuthResponseDTO registrar(RegistroRequestDTO dto) {

        if (dto.getAceptaTerminos() == null || !dto.getAceptaTerminos()) {
            throw new RuntimeException("Debe aceptar los términos y condiciones");
        }

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El correo ya está registrado");
        }

        validarPassword(dto.getPassword());

        Rol rol = rolRepository.findByNombre("TUTOR")
                .orElseThrow(() -> new RuntimeException("Rol TUTOR no encontrado — verifica que la base de datos esté inicializada"));

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setEmail(dto.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        usuario.setTelefono(dto.getTelefono());
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDate.now());

        usuarioRepository.save(usuario);

        String token = jwtUtil.generateToken(usuario.getEmail(), rol.getNombre(), usuario.getIdUsuario());
        return new AuthResponseDTO(token, usuario.getEmail(), rol.getNombre(), usuario.getIdUsuario(), usuario.getNombreCompleto());
    }

    /**
     * Autentica a un usuario con email y contraseña.
     *
     * @param dto email y contraseña del usuario
     * @return token JWT + email + nombre del rol
     * @throws RuntimeException si las credenciales son incorrectas o la cuenta está desactivada
     */
    public AuthResponseDTO login(LoginRequestDTO dto) {

        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (Boolean.TRUE.equals(usuario.getEliminado())) {
            throw new RuntimeException("CUENTA_ELIMINADA");
        }

        if (!usuario.getActivo()) {
            throw new RuntimeException("CUENTA_DESHABILITADA");
        }

        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getRol().getNombre(), usuario.getIdUsuario());
        return new AuthResponseDTO(token, usuario.getEmail(), usuario.getRol().getNombre(), usuario.getIdUsuario(), usuario.getNombreCompleto());
    }

    /**
     * Genera un token de recuperación de contraseña y lo envía al correo del usuario.
     * En modo dev ({@code mail.dev-mode=true}) el token se imprime en la consola del servidor.
     *
     * @param dto correo electrónico del usuario
     * @throws RuntimeException si el correo no está registrado
     */
    public void solicitarRecuperacion(RecuperarPasswordRequestDTO dto) {

        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Correo no registrado en el sistema"));

        String token = UUID.randomUUID().toString();
        usuario.setTokenRecuperacion(token);
        usuario.setFechaExpiracionToken(LocalDateTime.now().plusMinutes(30));
        usuarioRepository.save(usuario);

        emailService.enviarCorreoRecuperacion(usuario.getEmail(), token);
    }

    /**
     * Restablece la contraseña usando el token recibido por correo.
     * Aplica la misma política de contraseña que el registro.
     * El token se invalida tras el primer uso.
     *
     * @param dto token UUID y nueva contraseña
     * @throws RuntimeException si el token es inválido, expiró o la contraseña no cumple la política
     */
    public void restablecerPassword(NuevaPasswordRequestDTO dto) {

        Usuario usuario = usuarioRepository.findByTokenRecuperacion(dto.getToken())
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (usuario.getFechaExpiracionToken().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El token ha expirado");
        }

        validarPassword(dto.getNuevaPassword());

        usuario.setPasswordHash(passwordEncoder.encode(dto.getNuevaPassword()));
        usuario.setTokenRecuperacion(null);
        usuario.setFechaExpiracionToken(null);

        usuarioRepository.save(usuario);
    }

    /**
     * Cambia la contraseña del usuario autenticado, validando la contraseña actual.
     *
     * @param email          email del usuario autenticado (del JWT)
     * @param passwordActual contraseña vigente para verificación
     * @param passwordNueva  nueva contraseña; debe cumplir la política de seguridad
     * @throws RuntimeException si la contraseña actual es incorrecta o la nueva no cumple la política
     */
    public void cambiarPassword(String email, String passwordActual, String passwordNueva) {

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(passwordActual, usuario.getPasswordHash())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }

        validarPassword(passwordNueva);

        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
    }
}
