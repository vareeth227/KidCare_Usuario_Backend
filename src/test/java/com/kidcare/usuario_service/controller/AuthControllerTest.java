package com.kidcare.usuario_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidcare.usuario_service.dto.*;
import com.kidcare.usuario_service.security.JwtFilter;
import com.kidcare.usuario_service.security.JwtUtil;
import com.kidcare.usuario_service.security.SecurityConfig;
import com.kidcare.usuario_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, JwtFilter.class})
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ─── POST /api/auth/registro ──────────────────────────────────────────────

    @Test
    void registrar_ConDatosValidos_Retorna200Ok() throws Exception {
        RegistroRequestDTO requestDTO = new RegistroRequestDTO();
        requestDTO.setNombreCompleto("Ana Lopez");
        requestDTO.setEmail("ana@test.com");
        requestDTO.setPassword("Segura123!");
        requestDTO.setAceptaTerminos(true);

        AuthResponseDTO responseDTO = new AuthResponseDTO("jwt-token-xyz", "ana@test.com", "TUTOR", 1, "Ana Lopez");

        when(authService.registrar(any(RegistroRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-xyz"))
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.rol").value("TUTOR"));

        verify(authService).registrar(any(RegistroRequestDTO.class));
    }

    @Test
    void registrar_ConDatosInvalidos_Retorna400BadRequest() throws Exception {
        RegistroRequestDTO requestDTO = new RegistroRequestDTO();
        // Faltan campos obligatorios

        mockMvc.perform(post("/api/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }

    // ─── POST /api/auth/login ─────────────────────────────────────────────────

    @Test
    void login_ConCredencialesValidas_RetornaToken() throws Exception {
        LoginRequestDTO requestDTO = new LoginRequestDTO();
        requestDTO.setEmail("ana@test.com");
        requestDTO.setPassword("Segura123!");

        AuthResponseDTO responseDTO = new AuthResponseDTO("jwt-token-xyz", "ana@test.com", "TUTOR", 1, "Ana Lopez");

        when(authService.login(any(LoginRequestDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-xyz"));

        verify(authService).login(any(LoginRequestDTO.class));
    }

    // ─── POST /api/auth/recuperar ─────────────────────────────────────────────

    @Test
    void recuperar_ConEmailValido_RetornaMensaje() throws Exception {
        RecuperarPasswordRequestDTO requestDTO = new RecuperarPasswordRequestDTO();
        requestDTO.setEmail("ana@test.com");

        mockMvc.perform(post("/api/auth/recuperar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Correo de recuperación enviado. Revisa tu bandeja de entrada."));

        verify(authService).solicitarRecuperacion(any(RecuperarPasswordRequestDTO.class));
    }

    // ─── POST /api/auth/restablecer ───────────────────────────────────────────

    @Test
    void restablecer_ConTokenValido_RetornaMensaje() throws Exception {
        NuevaPasswordRequestDTO requestDTO = new NuevaPasswordRequestDTO();
        requestDTO.setToken("some-uuid-token");
        requestDTO.setNuevaPassword("NuevaClave123!");

        mockMvc.perform(post("/api/auth/restablecer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contraseña restablecida correctamente."));

        verify(authService).restablecerPassword(any(NuevaPasswordRequestDTO.class));
    }

    // ─── POST /api/auth/cambiar ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "ana@test.com", roles = {"TUTOR"})
    void cambiarPassword_Autenticado_RetornaMensaje() throws Exception {
        CambiarPasswordRequestDTO requestDTO = new CambiarPasswordRequestDTO();
        requestDTO.setPasswordActual("Segura123!");
        requestDTO.setPasswordNueva("NuevaPassword1!");

        mockMvc.perform(post("/api/auth/cambiar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Contraseña actualizada correctamente."));

        verify(authService).cambiarPassword(eq("ana@test.com"), eq("Segura123!"), eq("NuevaPassword1!"));
    }
}
