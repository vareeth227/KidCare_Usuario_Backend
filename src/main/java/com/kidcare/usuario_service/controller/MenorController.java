package com.kidcare.usuario_service.controller;

import com.kidcare.usuario_service.dto.MenorRequestDTO;
import com.kidcare.usuario_service.dto.MenorResponseDTO;
import com.kidcare.usuario_service.service.MenorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de perfiles de menores.
 *
 * <p>Todos los endpoints requieren JWT. El email del usuario autenticado se
 * extrae del {@link Authentication} para asociar los menores al tutor correcto.
 *
 * <p>Endpoints disponibles:
 * <ul>
 *   <li>POST /api/menores — crea un perfil de menor (TUTOR/ADMIN)</li>
 *   <li>GET /api/menores — lista los menores del usuario autenticado</li>
 *   <li>PUT /api/menores/{id} — edita un perfil de menor (TUTOR/ADMIN)</li>
 *   <li>DELETE /api/menores/{id} — elimina un perfil de menor (TUTOR/ADMIN)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/menores")
public class MenorController {

    @Autowired
    private MenorService menorService;

    /**
     * Crea un nuevo perfil de menor vinculado al tutor autenticado.
     *
     * @param dto datos del menor (nombre, fecha de nacimiento, sexo)
     * @param authentication contexto de seguridad; provee el email del tutor
     * @return 200 con los datos del menor creado
     */
    @PostMapping
    public ResponseEntity<MenorResponseDTO> crear(@Valid @RequestBody MenorRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(menorService.crearMenor(dto, authentication.getName()));
    }

    /**
     * Retorna todos los menores asociados al usuario autenticado.
     * Para TUTOR devuelve sus menores; para DELEGADO devuelve los menores a los que tiene acceso.
     *
     * @param authentication contexto de seguridad; provee el email del usuario
     * @return 200 con la lista de menores
     */
    @GetMapping
    public ResponseEntity<List<MenorResponseDTO>> listar(Authentication authentication) {
        return ResponseEntity.ok(menorService.obtenerMenoresPorTutor(authentication.getName()));
    }

    /**
     * Edita el perfil de un menor existente.
     *
     * @param id identificador del menor a editar
     * @param dto nuevos datos del menor
     * @param authentication contexto de seguridad; valida que el tutor sea propietario
     * @return 200 con los datos actualizados del menor
     */
    @PutMapping("/{id}")
    public ResponseEntity<MenorResponseDTO> editar(@PathVariable Integer id,
            @Valid @RequestBody MenorRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(menorService.editarMenor(id, dto, authentication.getName()));
    }

    // GET /api/menores/buscar?nombre=... — busca menores del tutor por nombre (CU004)
    @GetMapping("/buscar")
    public ResponseEntity<List<MenorResponseDTO>> buscar(@RequestParam String nombre,
            Authentication authentication) {
        return ResponseEntity.ok(menorService.buscarMenores(authentication.getName(), nombre));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenorResponseDTO> obtenerPorId(@PathVariable Integer id,
            Authentication authentication) {
        return ResponseEntity.ok(menorService.obtenerMenorPorId(id, authentication.getName()));
    }

    @PostMapping("/vincular/{idMenor}")
    public ResponseEntity<String> vincularExistente(@PathVariable Integer idMenor,
            Authentication authentication) {
        menorService.vincularTutorAMenorExistente(idMenor, authentication.getName());
        return ResponseEntity.ok("Menor vinculado correctamente");
    }

    /**
     * Elimina el perfil de un menor.
     *
     * @param id identificador del menor a eliminar
     * @param authentication contexto de seguridad; valida que el tutor sea propietario
     * @return 200 con mensaje de confirmación
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminar(@PathVariable Integer id,
            Authentication authentication) {
        menorService.eliminarMenor(id, authentication.getName());
        return ResponseEntity.ok("Menor eliminado correctamente");
    }
}