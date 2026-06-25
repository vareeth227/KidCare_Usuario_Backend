package com.kidcare.usuario_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidcare.usuario_service.dto.DelegadoResponseDTO;
import com.kidcare.usuario_service.dto.VincularDelegadoRequestDTO;
import com.kidcare.usuario_service.security.JwtFilter;
import com.kidcare.usuario_service.security.JwtUtil;
import com.kidcare.usuario_service.security.SecurityConfig;
import com.kidcare.usuario_service.service.DelegadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, JwtFilter.class})
@WebMvcTest(DelegadoController.class)
class DelegadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DelegadoService delegadoService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ─── POST /api/delegados/vincular ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void vincular_ConRolTutor_RetornaMensajeExito() throws Exception {
        VincularDelegadoRequestDTO requestDTO = new VincularDelegadoRequestDTO();
        requestDTO.setEmailDelegado("delegado@test.com");
        requestDTO.setIdMenor(10);
        requestDTO.setFechaExpiracion("2026-12-31");

        mockMvc.perform(post("/api/delegados/vincular")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(content().string("Apoderado vinculado correctamente"));

        verify(delegadoService).vincularDelegado(eq("tutor@test.com"), eq("delegado@test.com"), eq(10), eq("2026-12-31"));
    }

    @Test
    @WithMockUser(username = "delegado@test.com", roles = {"DELEGADO"})
    void vincular_ConRolDelegado_Retorna403Forbidden() throws Exception {
        VincularDelegadoRequestDTO requestDTO = new VincularDelegadoRequestDTO();
        requestDTO.setEmailDelegado("otro_delegado@test.com");
        requestDTO.setIdMenor(10);

        mockMvc.perform(post("/api/delegados/vincular")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    // ─── DELETE /api/delegados/desvincular/{idMenor} ─────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void desvincular_ConRolTutor_RetornaMensajeExito() throws Exception {
        mockMvc.perform(delete("/api/delegados/desvincular/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("Acceso revocado correctamente"));

        verify(delegadoService).desvincularDelegado(eq("tutor@test.com"), eq(10));
    }

    // ─── GET /api/delegados/menor/{idMenor} ──────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void obtenerDelegado_ConRolTutor_RetornaListaConDelegado() throws Exception {
        DelegadoResponseDTO responseDTO = new DelegadoResponseDTO();
        responseDTO.setEmail("delegado@test.com");
        responseDTO.setNombreCompleto("Carlos Delegado");

        when(delegadoService.obtenerDelegado(eq("tutor@test.com"), eq(10))).thenReturn(responseDTO);

        mockMvc.perform(get("/api/delegados/menor/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("delegado@test.com"))
                .andExpect(jsonPath("$[0].nombreCompleto").value("Carlos Delegado"));

        verify(delegadoService).obtenerDelegado(eq("tutor@test.com"), eq(10));
    }
}
