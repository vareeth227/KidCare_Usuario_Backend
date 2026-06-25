package com.kidcare.usuario_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidcare.usuario_service.dto.MenorRequestDTO;
import com.kidcare.usuario_service.dto.MenorResponseDTO;
import com.kidcare.usuario_service.security.JwtFilter;
import com.kidcare.usuario_service.security.JwtUtil;
import com.kidcare.usuario_service.security.SecurityConfig;
import com.kidcare.usuario_service.service.MenorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, JwtFilter.class})
@WebMvcTest(MenorController.class)
class MenorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MenorService menorService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ─── POST /api/menores ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void crear_ConDatosValidosYRolTutor_RetornaMenor() throws Exception {
        MenorRequestDTO requestDTO = new MenorRequestDTO();
        requestDTO.setNombre("Pedro");
        requestDTO.setFechaNacimiento(LocalDate.now().minusYears(5));
        requestDTO.setSexo("M");
        requestDTO.setEmoji("🦊");

        MenorResponseDTO responseDTO = new MenorResponseDTO();
        responseDTO.setIdMenor(1);
        responseDTO.setNombre("Pedro");
        responseDTO.setFechaNacimiento(requestDTO.getFechaNacimiento());
        responseDTO.setSexo("M");
        responseDTO.setEmoji("🦊");

        when(menorService.crearMenor(any(MenorRequestDTO.class), eq("tutor@test.com"))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/menores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMenor").value(1))
                .andExpect(jsonPath("$.nombre").value("Pedro"));

        verify(menorService).crearMenor(any(MenorRequestDTO.class), eq("tutor@test.com"));
    }

    @Test
    @WithMockUser(username = "delegado@test.com", roles = {"DELEGADO"})
    void crear_ConRolDelegado_Retorna403Forbidden() throws Exception {
        MenorRequestDTO requestDTO = new MenorRequestDTO();
        requestDTO.setNombre("Pedro");
        requestDTO.setFechaNacimiento(LocalDate.now().minusYears(5));
        requestDTO.setSexo("M");

        mockMvc.perform(post("/api/menores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/menores ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void listar_RetornaMenoresTutor() throws Exception {
        MenorResponseDTO m = new MenorResponseDTO();
        m.setIdMenor(1);
        m.setNombre("Pedro");

        when(menorService.obtenerMenoresPorTutor("tutor@test.com")).thenReturn(List.of(m));

        mockMvc.perform(get("/api/menores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idMenor").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Pedro"));

        verify(menorService).obtenerMenoresPorTutor("tutor@test.com");
    }

    // ─── PUT /api/menores/{id} ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void editar_ConDatosValidos_RetornaMenorEditado() throws Exception {
        MenorRequestDTO requestDTO = new MenorRequestDTO();
        requestDTO.setNombre("Pedro Editado");
        requestDTO.setFechaNacimiento(LocalDate.now().minusYears(5));
        requestDTO.setSexo("M");

        MenorResponseDTO responseDTO = new MenorResponseDTO();
        responseDTO.setIdMenor(1);
        responseDTO.setNombre("Pedro Editado");

        when(menorService.editarMenor(eq(1), any(MenorRequestDTO.class), eq("tutor@test.com"))).thenReturn(responseDTO);

        mockMvc.perform(put("/api/menores/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Pedro Editado"));

        verify(menorService).editarMenor(eq(1), any(MenorRequestDTO.class), eq("tutor@test.com"));
    }

    // ─── GET /api/menores/{id} ──────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void obtenerPorId_RetornaMenor() throws Exception {
        MenorResponseDTO m = new MenorResponseDTO();
        m.setIdMenor(1);
        m.setNombre("Pedro");

        when(menorService.obtenerMenorPorId(eq(1), eq("tutor@test.com"))).thenReturn(m);

        mockMvc.perform(get("/api/menores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idMenor").value(1))
                .andExpect(jsonPath("$.nombre").value("Pedro"));

        verify(menorService).obtenerMenorPorId(eq(1), eq("tutor@test.com"));
    }

    // ─── POST /api/menores/vincular/{idMenor} ───────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void vincular_RetornaMensajeExito() throws Exception {
        mockMvc.perform(post("/api/menores/vincular/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Menor vinculado correctamente"));

        verify(menorService).vincularTutorAMenorExistente(eq(1), eq("tutor@test.com"));
    }

    // ─── DELETE /api/menores/{id} ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void eliminar_RetornaMensajeExito() throws Exception {
        mockMvc.perform(delete("/api/menores/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Menor eliminado correctamente"));

        verify(menorService).eliminarMenor(eq(1), eq("tutor@test.com"));
    }

    @Test
    @WithMockUser(username = "delegado@test.com", roles = {"DELEGADO"})
    void eliminar_ConRolDelegado_Retorna403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/menores/1"))
                .andExpect(status().isForbidden());
    }
}
