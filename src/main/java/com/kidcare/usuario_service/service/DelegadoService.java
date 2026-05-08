package com.kidcare.usuario_service.service;

import com.kidcare.usuario_service.model.Menor;
import com.kidcare.usuario_service.model.Usuario;
import com.kidcare.usuario_service.model.UsuarioMenor;
import com.kidcare.usuario_service.model.UsuarioMenorId;
import com.kidcare.usuario_service.repository.MenorRepository;
import com.kidcare.usuario_service.repository.UsuarioMenorRepository;
import com.kidcare.usuario_service.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DelegadoService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioMenorRepository usuarioMenorRepository;

    @Autowired
    private MenorRepository menorRepository;

    public void vincularDelegado(String emailTutor, String emailDelegado, Integer idMenor) {

        Usuario tutor = usuarioRepository.findByEmail(emailTutor)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

        // Verifica que el tutor tenga acceso al menor
        UsuarioMenorId tutorMenorId = new UsuarioMenorId();
        tutorMenorId.setIdUsuario(tutor.getIdUsuario());
        tutorMenorId.setIdMenor(idMenor);
        if (!usuarioMenorRepository.existsById(tutorMenorId)) {
            throw new RuntimeException("No tienes acceso a este menor");
        }

        Usuario delegado = usuarioRepository.findByEmail(emailDelegado)
                .orElseThrow(() -> new RuntimeException("El apoderado no está registrado en el sistema"));

        if (!delegado.getRol().getNombre().equalsIgnoreCase("DELEGADO")) {
            throw new RuntimeException("El usuario no tiene rol de apoderado");
        }

        // Verifica que no esté vinculado ya
        UsuarioMenorId delegadoMenorId = new UsuarioMenorId();
        delegadoMenorId.setIdUsuario(delegado.getIdUsuario());
        delegadoMenorId.setIdMenor(idMenor);
        if (usuarioMenorRepository.existsById(delegadoMenorId)) {
            throw new RuntimeException("El apoderado ya tiene acceso a este menor");
        }

        Menor menor = menorRepository.findById(idMenor)
                .orElseThrow(() -> new RuntimeException("Menor no encontrado"));

        UsuarioMenor link = new UsuarioMenor();
        link.setId(delegadoMenorId);
        link.setUsuario(delegado);
        link.setMenor(menor);
        usuarioMenorRepository.save(link);
    }
}
