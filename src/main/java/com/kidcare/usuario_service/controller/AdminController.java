package com.kidcare.usuario_service.controller;

import com.kidcare.usuario_service.dto.AdminUsuarioResponseDTO;
import com.kidcare.usuario_service.dto.AuditoriaResponseDTO;
import com.kidcare.usuario_service.dto.CambiarRolDTO;
import com.kidcare.usuario_service.dto.CrearUsuarioAdminDTO;
import com.kidcare.usuario_service.dto.EditarUsuarioAdminDTO;
import com.kidcare.usuario_service.dto.MenorRequestDTO;
import com.kidcare.usuario_service.dto.MenorResponseDTO;
import com.kidcare.usuario_service.model.Usuario;
import com.kidcare.usuario_service.repository.UsuarioRepository;
import com.kidcare.usuario_service.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // CU015: Listar todos los usuarios del sistema
    @GetMapping("/usuarios")
    public ResponseEntity<List<AdminUsuarioResponseDTO>> listarUsuarios() {
        return ResponseEntity.ok(adminService.listarUsuarios());
    }

    // CU016: Habilitar cuenta deshabilitada
    @PatchMapping("/usuarios/{id}/habilitar")
    public ResponseEntity<Map<String, String>> habilitarCuenta(@PathVariable Integer id,
            Authentication authentication) {
        adminService.habilitarCuenta(id, obtenerIdAdmin(authentication));
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta habilitada correctamente"));
    }

    // CU017: Deshabilitar cuenta activa
    @PatchMapping("/usuarios/{id}/deshabilitar")
    public ResponseEntity<Map<String, String>> deshabilitarCuenta(@PathVariable Integer id,
            Authentication authentication) {
        adminService.deshabilitarCuenta(id, obtenerIdAdmin(authentication));
        return ResponseEntity.ok(Map.of("mensaje", "Cuenta deshabilitada correctamente"));
    }

    // CU018: Asignar nuevo rol a usuario
    @PatchMapping("/usuarios/{id}/rol")
    public ResponseEntity<Map<String, String>> asignarRol(@PathVariable Integer id,
            @Valid @RequestBody CambiarRolDTO dto,
            Authentication authentication) {
        adminService.asignarRol(id, dto.getIdRol(), obtenerIdAdmin(authentication));
        return ResponseEntity.ok(Map.of("mensaje", "Rol actualizado correctamente"));
    }

    // EP3: Crear usuario desde el admin
    @PostMapping("/usuarios")
    public ResponseEntity<AdminUsuarioResponseDTO> crearUsuario(@Valid @RequestBody CrearUsuarioAdminDTO dto,
            Authentication authentication) {
        AdminUsuarioResponseDTO creado = adminService.crearUsuario(dto, obtenerIdAdmin(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    // EP3: Editar usuario desde el admin
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<AdminUsuarioResponseDTO> editarUsuario(@PathVariable Integer id,
            @Valid @RequestBody EditarUsuarioAdminDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(adminService.editarUsuario(id, dto, obtenerIdAdmin(authentication)));
    }

    // EP3: Eliminar usuario con cascade
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Map<String, String>> eliminarUsuario(@PathVariable Integer id,
            Authentication authentication) {
        adminService.eliminarUsuario(id, obtenerIdAdmin(authentication));
        return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado correctamente"));
    }

    // EP3: Crear menor y vincularlo directamente a un usuario
    @PostMapping("/usuarios/{idUsuario}/menores")
    public ResponseEntity<MenorResponseDTO> crearMenorParaUsuario(@PathVariable Integer idUsuario,
            @Valid @RequestBody MenorRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.crearMenorParaUsuario(idUsuario, dto, obtenerIdAdmin(authentication)));
    }

    // EP3: Listar todos los menores del sistema
    @GetMapping("/menores")
    public ResponseEntity<List<MenorResponseDTO>> listarMenores() {
        return ResponseEntity.ok(adminService.listarMenores());
    }

    // EP3: Editar menor desde el admin (sin restricción de propietario)
    @PutMapping("/menores/{id}")
    public ResponseEntity<MenorResponseDTO> editarMenor(@PathVariable Integer id,
            @Valid @RequestBody MenorRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(adminService.editarMenor(id, dto, obtenerIdAdmin(authentication)));
    }

    // EP3: Eliminar menor con cascade desde el admin
    @DeleteMapping("/menores/{id}")
    public ResponseEntity<Map<String, String>> eliminarMenor(@PathVariable Integer id,
            Authentication authentication) {
        adminService.eliminarMenor(id, obtenerIdAdmin(authentication));
        return ResponseEntity.ok(Map.of("mensaje", "Menor eliminado correctamente"));
    }

    // EP3: Asociar usuario existente a un menor existente
    @PostMapping("/menores/{idMenor}/vincular/{idUsuario}")
    public ResponseEntity<Map<String, String>> asociarUsuarioMenor(@PathVariable Integer idMenor,
            @PathVariable Integer idUsuario,
            Authentication authentication) {
        adminService.asociarUsuarioMenor(idMenor, idUsuario, obtenerIdAdmin(authentication));
        return ResponseEntity.ok(Map.of("mensaje", "Usuario vinculado al menor correctamente"));
    }

    // CU019: Consultar auditoría con filtros opcionales
    @GetMapping("/auditoria")
    public ResponseEntity<List<AuditoriaResponseDTO>> consultarAuditoria(
            @RequestParam(required = false) String cambio,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(adminService.consultarAuditoria(cambio, entidad, desde, hasta));
    }

    private Integer obtenerIdAdmin(Authentication authentication) {
        String email = authentication.getName();
        Usuario admin = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));
        return admin.getIdUsuario();
    }
}
