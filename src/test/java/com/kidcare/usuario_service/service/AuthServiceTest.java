package com.kidcare.usuario_service.service;

import com.kidcare.usuario_service.dto.AuthResponseDTO;
import com.kidcare.usuario_service.dto.LoginRequestDTO;
import com.kidcare.usuario_service.dto.RegistroRequestDTO;
import com.kidcare.usuario_service.model.Rol;
import com.kidcare.usuario_service.model.Usuario;
import com.kidcare.usuario_service.repository.RolRepository;
import com.kidcare.usuario_service.repository.UsuarioRepository;
import com.kidcare.usuario_service.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock RolRepository rolRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock EmailService emailService;
    @InjectMocks AuthService authService;

    // ─── registrar ────────────────────────────────────────────────────────────

    @Test
    void registrar_conDatosValidos_retornaToken() {
        RegistroRequestDTO dto = new RegistroRequestDTO();
        dto.setNombreCompleto("Juan Pérez");
        dto.setEmail("juan@test.com");
        dto.setPassword("Segura@123");
        dto.setAceptaTerminos(true);

        Rol rol = new Rol();
        rol.setNombre("TUTOR");

        when(usuarioRepository.existsByEmail("juan@test.com")).thenReturn(false);
        when(rolRepository.findByNombre("TUTOR")).thenReturn(Optional.of(rol));
        when(passwordEncoder.encode(anyString())).thenReturn("hash_bcrypt");
        when(jwtUtil.generateToken(anyString(), anyString(), any())).thenReturn("jwt.token");
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponseDTO resultado = authService.registrar(dto);

        assertThat(resultado.getToken()).isEqualTo("jwt.token");
        assertThat(resultado.getEmail()).isEqualTo("juan@test.com");
        assertThat(resultado.getRol()).isEqualTo("TUTOR");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void registrar_emailDuplicado_lanzaExcepcion() {
        RegistroRequestDTO dto = new RegistroRequestDTO();
        dto.setNombreCompleto("Juan");
        dto.setEmail("juan@test.com");
        dto.setPassword("Segura@123");
        dto.setAceptaTerminos(true);

        when(usuarioRepository.existsByEmail("juan@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya está registrado");
    }

    @Test
    void registrar_sinAceptarTerminos_lanzaExcepcion() {
        RegistroRequestDTO dto = new RegistroRequestDTO();
        dto.setNombreCompleto("Juan");
        dto.setEmail("juan@test.com");
        dto.setPassword("Segura@123");
        dto.setAceptaTerminos(false);

        assertThatThrownBy(() -> authService.registrar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("términos");
    }

    @Test
    void registrar_passwordMuyCorta_lanzaExcepcion() {
        RegistroRequestDTO dto = new RegistroRequestDTO();
        dto.setNombreCompleto("Juan");
        dto.setEmail("juan@test.com");
        dto.setPassword("Ab@1");
        dto.setAceptaTerminos(true);

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.registrar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("8 caracteres");
    }

    @Test
    void registrar_passwordSinMayuscula_lanzaExcepcion() {
        RegistroRequestDTO dto = new RegistroRequestDTO();
        dto.setNombreCompleto("Juan");
        dto.setEmail("juan@test.com");
        dto.setPassword("sinmayuscula@1");
        dto.setAceptaTerminos(true);

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.registrar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayúscula");
    }

    @Test
    void registrar_passwordSinSimbolo_lanzaExcepcion() {
        RegistroRequestDTO dto = new RegistroRequestDTO();
        dto.setNombreCompleto("Juan");
        dto.setEmail("juan@test.com");
        dto.setPassword("SinSimbolo1234");
        dto.setAceptaTerminos(true);

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.registrar(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("símbolo");
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    void login_credencialesValidas_retornaToken() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("juan@test.com");
        dto.setPassword("Segura@123");

        Rol rol = new Rol();
        rol.setNombre("TUTOR");

        Usuario usuario = new Usuario();
        usuario.setEmail("juan@test.com");
        usuario.setPasswordHash("hash_bcrypt");
        usuario.setActivo(true);
        usuario.setRol(rol);

        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("Segura@123", "hash_bcrypt")).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), anyString(), any())).thenReturn("jwt.token");

        AuthResponseDTO resultado = authService.login(dto);

        assertThat(resultado.getToken()).isEqualTo("jwt.token");
        assertThat(resultado.getRol()).isEqualTo("TUTOR");
    }

    @Test
    void login_emailNoRegistrado_lanzaExcepcion() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("noexiste@test.com");
        dto.setPassword("cualquier");

        when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credenciales incorrectas");
    }

    @Test
    void login_cuentaDesactivada_lanzaExcepcion() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("juan@test.com");
        dto.setPassword("cualquier");

        Usuario usuario = new Usuario();
        usuario.setActivo(false);

        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("desactivada");
    }

    @Test
    void login_passwordIncorrecta_lanzaExcepcion() {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail("juan@test.com");
        dto.setPassword("incorrecta");

        Rol rol = new Rol();
        rol.setNombre("TUTOR");

        Usuario usuario = new Usuario();
        usuario.setActivo(true);
        usuario.setPasswordHash("hash_bcrypt");
        usuario.setRol(rol);

        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "hash_bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credenciales incorrectas");
    }

    // ─── cambiarPassword ──────────────────────────────────────────────────────

    @Test
    void cambiarPassword_exitoso_actualizaHash() {
        Usuario usuario = new Usuario();
        usuario.setPasswordHash("hashViejo");

        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("actual123", "hashViejo")).thenReturn(true);
        when(passwordEncoder.encode("Nueva@456")).thenReturn("hashNuevo");

        authService.cambiarPassword("juan@test.com", "actual123", "Nueva@456");

        assertThat(usuario.getPasswordHash()).isEqualTo("hashNuevo");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void cambiarPassword_passwordActualIncorrecta_lanzaExcepcion() {
        Usuario usuario = new Usuario();
        usuario.setPasswordHash("hashViejo");

        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", "hashViejo")).thenReturn(false);

        assertThatThrownBy(() -> authService.cambiarPassword("juan@test.com", "incorrecta", "Nueva@456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("contraseña actual es incorrecta");
    }

    @Test
    void cambiarPassword_nuevaPasswordInvalida_lanzaExcepcion() {
        Usuario usuario = new Usuario();
        usuario.setPasswordHash("hashViejo");

        when(usuarioRepository.findByEmail("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("actual123", "hashViejo")).thenReturn(true);

        assertThatThrownBy(() -> authService.cambiarPassword("juan@test.com", "actual123", "sinSimbolo123"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("símbolo");
    }

    @Test
    void cambiarPassword_usuarioNoEncontrado_lanzaExcepcion() {
        when(usuarioRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.cambiarPassword("ghost@test.com", "x", "Nueva@1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no encontrado");
    }
}
