package com.kidcare.usuario_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidcare.usuario_service.dto.CompletarRegistroDTO;
import com.kidcare.usuario_service.dto.InvitacionRequestDTO;
import com.kidcare.usuario_service.security.JwtFilter;
import com.kidcare.usuario_service.security.JwtUtil;
import com.kidcare.usuario_service.security.SecurityConfig;
import com.kidcare.usuario_service.service.InvitacionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, JwtFilter.class})
@WebMvcTest(InvitacionController.class)
class InvitacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvitacionService invitacionService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ─── POST /api/invitaciones/enviar ─────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void enviar_ConRolTutor_RetornaMensajeExito() throws Exception {
        InvitacionRequestDTO requestDTO = new InvitacionRequestDTO();
        requestDTO.setEmailDelegado("delegado_inv@test.com");
        requestDTO.setIdMenor(10);

        mockMvc.perform(post("/api/invitaciones/enviar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Invitación enviada correctamente"));

        verify(invitacionService).enviarInvitacion(eq("tutor@test.com"), any(InvitacionRequestDTO.class));
    }

    @Test
    @WithMockUser(username = "delegado@test.com", roles = {"DELEGADO"})
    void enviar_ConRolDelegado_Retorna403Forbidden() throws Exception {
        InvitacionRequestDTO requestDTO = new InvitacionRequestDTO();
        requestDTO.setEmailDelegado("delegado_inv@test.com");
        requestDTO.setIdMenor(10);

        mockMvc.perform(post("/api/invitaciones/enviar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    // ─── POST /api/invitaciones/completar (PermitAll) ───────────────────────────

    @Test
    void completar_SinAutenticacion_Permitido_RetornaMensajeExito() throws Exception {
        CompletarRegistroDTO requestDTO = new CompletarRegistroDTO();
        requestDTO.setToken("uuid-token-xyz");
        requestDTO.setNombreCompleto("Carlos Invitado");
        requestDTO.setPassword("Password123!");

        mockMvc.perform(post("/api/invitaciones/completar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Registro completado. Ya puedes iniciar sesión."));

        verify(invitacionService).completarRegistro(any(CompletarRegistroDTO.class));
    }
}
