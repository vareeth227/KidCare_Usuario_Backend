package com.kidcare.usuario_service.controller;

import com.kidcare.usuario_service.dto.*;
import com.kidcare.usuario_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación y gestión de cuentas de usuario.
 *
 * <p>Todos los endpoints son públicos (no requieren JWT) y están declarados
 * en {@link com.kidcare.usuario_service.security.SecurityConfig} como {@code permitAll}.
 *
 * <p>Endpoints disponibles:
 * <ul>
 *   <li>POST /api/auth/registro — crea nueva cuenta (TUTOR o DELEGADO)</li>
 *   <li>POST /api/auth/login — autentica y retorna JWT</li>
 *   <li>POST /api/auth/recuperar — envía token de recuperación por correo</li>
 *   <li>POST /api/auth/restablecer — establece nueva contraseña con el token</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Registra un nuevo usuario y retorna un JWT listo para usar.
     *
     * @param dto nombre completo, email, contraseña, aceptaTerminos, rolNombre (TUTOR|DELEGADO)
     * @return 200 con token JWT, email y rol — 400 si el email ya existe
     */
    @PostMapping("/registro")
    public ResponseEntity<AuthResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO dto) {
        return ResponseEntity.ok(authService.registrar(dto));
    }

    /**
     * Autentica al usuario con email y contraseña.
     *
     * @param dto email y contraseña
     * @return 200 con token JWT, email y rol — 400 si las credenciales son incorrectas
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    /**
     * Solicita recuperación de contraseña.
     * Genera un token UUID y lo envía al correo del usuario vía Gmail SMTP.
     *
     * @param dto correo electrónico registrado en el sistema
     * @return 200 con mensaje de confirmación — 400 si el correo no existe
     */
    @PostMapping("/recuperar")
    public ResponseEntity<String> recuperar(@Valid @RequestBody RecuperarPasswordRequestDTO dto) {
        authService.solicitarRecuperacion(dto);
        return ResponseEntity.ok("Correo de recuperación enviado. Revisa tu bandeja de entrada.");
    }

    /**
     * Restablece la contraseña usando el token recibido por correo.
     * El token se invalida después de usarse para evitar reutilización.
     *
     * @param dto token de recuperación y nueva contraseña
     * @return 200 con mensaje de confirmación — 400 si el token es inválido o expiró
     */
    @PostMapping("/restablecer")
    public ResponseEntity<String> restablecer(@Valid @RequestBody NuevaPasswordRequestDTO dto) {
        authService.restablecerPassword(dto);
        return ResponseEntity.ok("Contraseña restablecida correctamente.");
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     * Requiere JWT válido; valida la contraseña actual antes de aplicar el cambio.
     *
     * @param dto             contraseña actual y nueva contraseña
     * @param authentication  contexto de seguridad con el email del usuario
     * @return 200 con mensaje de confirmación — 400 si la contraseña actual es incorrecta o la nueva no cumple la política
     */
    @PostMapping("/cambiar")
    public ResponseEntity<String> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequestDTO dto,
            Authentication authentication) {
        authService.cambiarPassword(authentication.getName(), dto.getPasswordActual(), dto.getPasswordNueva());
        return ResponseEntity.ok("Contraseña actualizada correctamente.");
    }

}
