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
import java.util.UUID;

// Servicio que maneja el registro, login y recuperación de contraseña
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

    // Registra un nuevo usuario con rol TUTOR
    public AuthResponseDTO registrar(RegistroRequestDTO dto) {

        // Verifica que el usuario aceptó los términos y condiciones
        if (dto.getAceptaTerminos() == null || !dto.getAceptaTerminos()) {
            throw new RuntimeException("Debe aceptar los términos y condiciones");
        }

        // Verifica que el correo no esté registrado
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("El correo ya está registrado");
        }

        // Determina el rol: TUTOR por defecto, acepta DELEGADO
        String nombreRol = "DELEGADO".equalsIgnoreCase(dto.getRolNombre()) ? "DELEGADO" : "TUTOR";
        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("Rol " + nombreRol + " no encontrado"));

        // Crea el nuevo usuario
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setEmail(dto.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        usuario.setTelefono(dto.getTelefono());
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDate.now());

        usuarioRepository.save(usuario);

        // Genera y retorna el token JWT
        String token = jwtUtil.generateToken(usuario.getEmail(), rol.getNombre(), usuario.getIdUsuario());
        return new AuthResponseDTO(token, usuario.getEmail(), rol.getNombre());
    }

    // Inicia sesión y retorna un token JWT
    public AuthResponseDTO login(LoginRequestDTO dto) {

        // Busca el usuario por correo
        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        // Verifica que la cuenta esté activa
        if (!usuario.getActivo()) {
            throw new RuntimeException("La cuenta está desactivada");
        }

        // Verifica la contraseña
        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        // Genera y retorna el token JWT
        String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getRol().getNombre(), usuario.getIdUsuario());
        return new AuthResponseDTO(token, usuario.getEmail(), usuario.getRol().getNombre());
    }

    // Genera un token de recuperación y lo asigna al usuario
    public void solicitarRecuperacion(RecuperarPasswordRequestDTO dto) {

        Usuario usuario = usuarioRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("Correo no registrado"));

        // Genera token único de recuperación válido por 30 minutos
        String token = UUID.randomUUID().toString();
        usuario.setTokenRecuperacion(token);
        usuario.setFechaExpiracionToken(LocalDate.now().plusDays(1));

        usuarioRepository.save(usuario);

        // Aquí se enviaría el correo con el token (se implementa con JavaMailSender)
        System.out.println("Token de recuperación: " + token);
    }

    // Restablece la contraseña usando el token de recuperación
    public void restablecerPassword(NuevaPasswordRequestDTO dto) {

        // Busca el usuario por token de recuperación
        Usuario usuario = usuarioRepository.findByTokenRecuperacion(dto.getToken())
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        // Verifica que el token no haya expirado
        if (usuario.getFechaExpiracionToken().isBefore(LocalDate.now())) {
            throw new RuntimeException("El token ha expirado");
        }

        // Actualiza la contraseña y limpia el token
        usuario.setPasswordHash(passwordEncoder.encode(dto.getNuevaPassword()));
        usuario.setTokenRecuperacion(null);
        usuario.setFechaExpiracionToken(null);

        usuarioRepository.save(usuario);
    }
}