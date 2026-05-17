package com.kidcare.usuario_service.controller;

import com.kidcare.usuario_service.dto.CompletarRegistroDTO;
import com.kidcare.usuario_service.dto.InvitacionRequestDTO;
import com.kidcare.usuario_service.service.InvitacionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/invitaciones")
public class InvitacionController {

    @Autowired
    private InvitacionService invitacionService;

    @PostMapping("/enviar")
    public ResponseEntity<Map<String, String>> enviar(@Valid @RequestBody InvitacionRequestDTO dto,
            Authentication authentication) {
        invitacionService.enviarInvitacion(authentication.getName(), dto);
        return ResponseEntity.ok(Map.of("mensaje", "Invitación enviada correctamente"));
    }

    @PostMapping("/completar")
    public ResponseEntity<Map<String, String>> completar(@Valid @RequestBody CompletarRegistroDTO dto) {
        invitacionService.completarRegistro(dto);
        return ResponseEntity.ok(Map.of("mensaje", "Registro completado. Ya puedes iniciar sesión."));
    }
}
