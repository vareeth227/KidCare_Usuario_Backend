package com.kidcare.usuario_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kidcare.usuario_service.dto.AdminUsuarioResponseDTO;
import com.kidcare.usuario_service.dto.AuditoriaResponseDTO;
import com.kidcare.usuario_service.dto.CambiarRolDTO;
import com.kidcare.usuario_service.model.Usuario;
import com.kidcare.usuario_service.repository.UsuarioRepository;
import com.kidcare.usuario_service.service.AdminService;
import com.kidcare.usuario_service.security.JwtFilter;
import com.kidcare.usuario_service.security.JwtUtil;
import com.kidcare.usuario_service.security.SecurityConfig;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, JwtFilter.class})
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    // Helper to setup mock Admin user in Repository
    private void mockAdminUser() {
        Usuario adminUser = new Usuario();
        adminUser.setIdUsuario(999);
        adminUser.setEmail("admin@test.com");
        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
    }

    // ─── GET /api/admin/usuarios ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void listarUsuarios_ConRolAdmin_RetornaLista() throws Exception {
        AdminUsuarioResponseDTO u = new AdminUsuarioResponseDTO();
        u.setIdUsuario(1);
        u.setEmail("user@test.com");
        u.setNombreCompleto("Juan Perez");
        u.setActivo(true);
        u.setRol("TUTOR");

        when(adminService.listarUsuarios()).thenReturn(List.of(u));

        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("user@test.com"))
                .andExpect(jsonPath("$[0].nombreCompleto").value("Juan Perez"));

        verify(adminService).listarUsuarios();
    }

    @Test
    @WithMockUser(username = "tutor@test.com", roles = {"TUTOR"})
    void listarUsuarios_ConRolTutor_Retorna403Forbidden() throws Exception {
        mockMvc.perform(get("/api/admin/usuarios"))
                .andExpect(status().isForbidden());
    }

    // ─── PATCH /api/admin/usuarios/{id}/habilitar ────────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void habilitarCuenta_ConRolAdmin_RetornaMensajeExito() throws Exception {
        mockAdminUser();

        mockMvc.perform(patch("/api/admin/usuarios/10/habilitar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Cuenta habilitada correctamente"));

        verify(adminService).habilitarCuenta(eq(10), eq(999));
    }

    // ─── PATCH /api/admin/usuarios/{id}/deshabilitar ─────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void deshabilitarCuenta_ConRolAdmin_RetornaMensajeExito() throws Exception {
        mockAdminUser();

        mockMvc.perform(patch("/api/admin/usuarios/10/deshabilitar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Cuenta deshabilitada correctamente"));

        verify(adminService).deshabilitarCuenta(eq(10), eq(999));
    }

    // ─── PATCH /api/admin/usuarios/{id}/rol ──────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void asignarRol_ConRolAdmin_RetornaMensajeExito() throws Exception {
        mockAdminUser();

        CambiarRolDTO dto = new CambiarRolDTO();
        dto.setIdRol(2);

        mockMvc.perform(patch("/api/admin/usuarios/10/rol")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Rol actualizado correctamente"));

        verify(adminService).asignarRol(eq(10), eq(2), eq(999));
    }

    // ─── GET /api/admin/auditoria ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void consultarAuditoria_ConRolAdmin_RetornaListaLogs() throws Exception {
        AuditoriaResponseDTO log = new AuditoriaResponseDTO();
        log.setIdAuditoria(500);
        log.setCambio("Usuario deshabilitado");

        when(adminService.consultarAuditoria(any(), any(), any(), any())).thenReturn(List.of(log));

        mockMvc.perform(get("/api/admin/auditoria?cambio=DESHABILITAR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idAuditoria").value(500))
                .andExpect(jsonPath("$[0].cambio").value("Usuario deshabilitado"));

        verify(adminService).consultarAuditoria(eq("DESHABILITAR"), any(), any(), any());
    }
}
