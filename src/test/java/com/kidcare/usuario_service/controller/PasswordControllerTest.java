package com.kidcare.usuario_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidcare.usuario_service.dto.NuevaPasswordRequestDTO;
import com.kidcare.usuario_service.dto.RecuperarPasswordRequestDTO;
import com.kidcare.usuario_service.security.JwtFilter;
import com.kidcare.usuario_service.security.JwtUtil;
import com.kidcare.usuario_service.security.SecurityConfig;
import com.kidcare.usuario_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, JwtFilter.class})
@WebMvcTest(PasswordController.class)
class PasswordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ─── POST /api/password/recuperar ──────────────────────────────────────────

    @Test
    void recuperar_SinAutenticacion_Permitido_RetornaExito() throws Exception {
        RecuperarPasswordRequestDTO requestDTO = new RecuperarPasswordRequestDTO();
        requestDTO.setEmail("ana@test.com");

        mockMvc.perform(post("/api/password/recuperar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("Token de recuperación enviado"));

        verify(authService).solicitarRecuperacion(any(RecuperarPasswordRequestDTO.class));
    }

    // ─── POST /api/password/restablecer ─────────────────────────────────────────

    @Test
    void restablecer_SinAutenticacion_Permitido_RetornaExito() throws Exception {
        NuevaPasswordRequestDTO requestDTO = new NuevaPasswordRequestDTO();
        requestDTO.setToken("uuid-token-xyz");
        requestDTO.setNuevaPassword("NuevaPassword123!");

        mockMvc.perform(post("/api/password/restablecer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("Contraseña restablecida correctamente"));

        verify(authService).restablecerPassword(any(NuevaPasswordRequestDTO.class));
    }
}
