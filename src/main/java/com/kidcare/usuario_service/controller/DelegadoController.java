package com.kidcare.usuario_service.controller;

import com.kidcare.usuario_service.dto.DelegadoResponseDTO;
import com.kidcare.usuario_service.dto.VincularDelegadoRequestDTO;
import com.kidcare.usuario_service.service.DelegadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controlador REST para la gestión de apoderados vinculados a menores.
 *
 * <p>Solo accesible para usuarios con rol TUTOR o ADMIN según las reglas
 * definidas en {@link com.kidcare.usuario_service.security.SecurityConfig}.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST   /api/delegados/vincular          — asigna un apoderado al menor</li>
 *   <li>DELETE /api/delegados/desvincular/{id}  — revoca el acceso del apoderado</li>
 *   <li>GET    /api/delegados/menor/{id}        — consulta el apoderado actual</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/delegados")
public class DelegadoController {

    @Autowired
    private DelegadoService delegadoService;

    /**
     * Vincula un usuario como apoderado de un menor del tutor autenticado.
     * Solo se permite un apoderado por menor a la vez.
     *
     * @param dto              email del apoderado, ID del menor y fecha de expiración opcional
     * @param authentication   contexto de seguridad; extrae el email del tutor
     * @return 200 con mensaje de confirmación
     */
    @PostMapping("/vincular")
    public ResponseEntity<String> vincular(@RequestBody VincularDelegadoRequestDTO dto,
            Authentication authentication) {
        delegadoService.vincularDelegado(
                authentication.getName(),
                dto.getEmailDelegado(),
                dto.getIdMenor(),
                dto.getFechaExpiracion());
        return ResponseEntity.ok("Apoderado vinculado correctamente");
    }

    /**
     * Revoca el acceso del apoderado actual al menor indicado.
     *
     * @param idMenor        ID del menor
     * @param authentication contexto de seguridad; extrae el email del tutor
     * @return 200 con mensaje de confirmación
     */
    @DeleteMapping("/desvincular/{idMenor}")
    public ResponseEntity<String> desvincular(@PathVariable Integer idMenor,
            Authentication authentication) {
        delegadoService.desvincularDelegado(authentication.getName(), idMenor);
        return ResponseEntity.ok("Acceso revocado correctamente");
    }

    /**
     * Devuelve el apoderado actualmente vinculado al menor.
     *
     * @param idMenor        ID del menor
     * @param authentication contexto de seguridad; extrae el email del tutor
     * @return 200 con datos del apoderado, o 204 si no hay ninguno asignado
     */
    @GetMapping("/menor/{idMenor}")
    public ResponseEntity<List<DelegadoResponseDTO>> obtenerDelegado(@PathVariable Integer idMenor,
            Authentication authentication) {
        DelegadoResponseDTO dto = delegadoService.obtenerDelegado(authentication.getName(), idMenor);
        List<DelegadoResponseDTO> lista = dto != null ? List.of(dto) : List.of();
        return ResponseEntity.ok(lista);
    }
}
