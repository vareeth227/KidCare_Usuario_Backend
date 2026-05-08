package com.kidcare.usuario_service.controller;

import com.kidcare.usuario_service.dto.VincularDelegadoRequestDTO;
import com.kidcare.usuario_service.service.DelegadoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delegados")
public class DelegadoController {

    @Autowired
    private DelegadoService delegadoService;

    // POST /api/delegados/vincular — el tutor vincula un apoderado a un menor
    @PostMapping("/vincular")
    public ResponseEntity<String> vincular(@RequestBody VincularDelegadoRequestDTO dto,
            Authentication authentication) {
        delegadoService.vincularDelegado(authentication.getName(), dto.getEmailDelegado(), dto.getIdMenor());
        return ResponseEntity.ok("Apoderado vinculado correctamente");
    }
}
