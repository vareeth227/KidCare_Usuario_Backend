package com.kidcare.usuario_service.service;

import com.kidcare.usuario_service.dto.DelegadoResponseDTO;
import com.kidcare.usuario_service.model.Menor;
import com.kidcare.usuario_service.model.Rol;
import com.kidcare.usuario_service.model.Usuario;
import com.kidcare.usuario_service.model.UsuarioMenor;
import com.kidcare.usuario_service.model.UsuarioMenorId;
import com.kidcare.usuario_service.repository.MenorRepository;
import com.kidcare.usuario_service.repository.RolRepository;
import com.kidcare.usuario_service.repository.UsuarioMenorRepository;
import com.kidcare.usuario_service.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de negocio para la vinculación de apoderados a menores.
 *
 * <p>Reglas de negocio:
 * <ul>
 *   <li>Solo un apoderado por menor a la vez (seguridad).</li>
 *   <li>El tutor debe ser propietario del menor.</li>
 *   <li>Un usuario TUTOR/ADMIN conserva su rol; cualquier otro recibe rol DELEGADO.</li>
 *   <li>El acceso puede tener fecha de expiración opcional.</li>
 * </ul>
 */
@Service
public class DelegadoService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UsuarioMenorRepository usuarioMenorRepository;
    @Autowired private MenorRepository menorRepository;
    @Autowired private RolRepository rolRepository;

    /**
     * Vincula un usuario como apoderado de un menor del tutor autenticado.
     * Lanza excepción si el menor ya tiene apoderado asignado.
     *
     * @param emailTutor       email del tutor autenticado
     * @param emailDelegado    email del usuario a vincular
     * @param idMenor          ID del menor
     * @param fechaExpiracionStr fecha límite "YYYY-MM-DD"; null para acceso permanente
     */
    @Transactional
    public void vincularDelegado(String emailTutor, String emailDelegado,
                                  Integer idMenor, String fechaExpiracionStr) {

        Usuario tutor = usuarioRepository.findByEmail(emailTutor)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

        UsuarioMenorId tutorMenorId = new UsuarioMenorId();
        tutorMenorId.setIdUsuario(tutor.getIdUsuario());
        tutorMenorId.setIdMenor(idMenor);
        if (!usuarioMenorRepository.existsById(tutorMenorId)) {
            throw new RuntimeException("No tienes acceso a este menor");
        }

        if (emailDelegado.equalsIgnoreCase(emailTutor)) {
            throw new RuntimeException("No puedes asignarte acceso a ti mismo");
        }

        // Regla: solo un apoderado por menor a la vez
        List<UsuarioMenor> existentes = usuarioMenorRepository
                .findByIdIdMenorAndIdIdUsuarioNot(idMenor, tutor.getIdUsuario());
        if (!existentes.isEmpty()) {
            throw new RuntimeException(
                "Este menor ya tiene un apoderado asignado. Revoca el acceso actual antes de asignar uno nuevo.");
        }

        Usuario delegado = usuarioRepository.findByEmail(emailDelegado)
                .orElseThrow(() -> new RuntimeException("El usuario no está registrado en el sistema"));

        // TUTOR y ADMIN conservan su rol; los demás reciben rol DELEGADO
        String rolActual = delegado.getRol().getNombre();
        if (!rolActual.equalsIgnoreCase("TUTOR") && !rolActual.equalsIgnoreCase("ADMIN")) {
            Rol rolDelegado = rolRepository.findByNombre("DELEGADO")
                    .orElseThrow(() -> new RuntimeException("Rol DELEGADO no encontrado"));
            delegado.setRol(rolDelegado);
            usuarioRepository.save(delegado);
        }

        UsuarioMenorId delegadoMenorId = new UsuarioMenorId();
        delegadoMenorId.setIdUsuario(delegado.getIdUsuario());
        delegadoMenorId.setIdMenor(idMenor);

        Menor menor = menorRepository.findById(idMenor)
                .orElseThrow(() -> new RuntimeException("Menor no encontrado"));

        LocalDate fechaExpiracion = (fechaExpiracionStr != null && !fechaExpiracionStr.isBlank())
                ? LocalDate.parse(fechaExpiracionStr) : null;

        UsuarioMenor link = new UsuarioMenor();
        link.setId(delegadoMenorId);
        link.setUsuario(delegado);
        link.setMenor(menor);
        link.setFechaExpiracion(fechaExpiracion);
        usuarioMenorRepository.save(link);
    }

    /**
     * Revoca el acceso del apoderado actual al menor indicado.
     *
     * @param emailTutor email del tutor autenticado
     * @param idMenor    ID del menor
     */
    @Transactional
    public void desvincularDelegado(String emailTutor, Integer idMenor) {

        Usuario tutor = usuarioRepository.findByEmail(emailTutor)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

        UsuarioMenorId tutorMenorId = new UsuarioMenorId();
        tutorMenorId.setIdUsuario(tutor.getIdUsuario());
        tutorMenorId.setIdMenor(idMenor);
        if (!usuarioMenorRepository.existsById(tutorMenorId)) {
            throw new RuntimeException("No tienes acceso a este menor");
        }

        List<UsuarioMenor> delegados = usuarioMenorRepository
                .findByIdIdMenorAndIdIdUsuarioNot(idMenor, tutor.getIdUsuario());
        if (delegados.isEmpty()) {
            throw new RuntimeException("Este menor no tiene un apoderado asignado");
        }
        usuarioMenorRepository.deleteAll(delegados);
    }

    /**
     * Devuelve el apoderado actual del menor, o null si no tiene ninguno.
     *
     * @param emailTutor email del tutor autenticado
     * @param idMenor    ID del menor
     * @return datos del apoderado o null
     */
    public DelegadoResponseDTO obtenerDelegado(String emailTutor, Integer idMenor) {

        Usuario tutor = usuarioRepository.findByEmail(emailTutor)
                .orElseThrow(() -> new RuntimeException("Tutor no encontrado"));

        UsuarioMenorId tutorMenorId = new UsuarioMenorId();
        tutorMenorId.setIdUsuario(tutor.getIdUsuario());
        tutorMenorId.setIdMenor(idMenor);
        if (!usuarioMenorRepository.existsById(tutorMenorId)) {
            throw new RuntimeException("No tienes acceso a este menor");
        }

        List<UsuarioMenor> links = usuarioMenorRepository
                .findByIdIdMenorAndIdIdUsuarioNot(idMenor, tutor.getIdUsuario());
        if (links.isEmpty()) return null;

        UsuarioMenor link = links.get(0);
        Usuario delegado = link.getUsuario();
        String fechaExp = link.getFechaExpiracion() != null
                ? link.getFechaExpiracion().toString() : null;

        DelegadoResponseDTO dto = new DelegadoResponseDTO();
        dto.setIdUsuario(delegado.getIdUsuario());
        dto.setIdDelegado(delegado.getIdUsuario());
        dto.setEmail(delegado.getEmail());
        dto.setEmailDelegado(delegado.getEmail());
        dto.setNombreCompleto(delegado.getNombreCompleto());
        dto.setNombreDelegado(delegado.getNombreCompleto());
        dto.setEstado("ACTIVO");
        dto.setDuracion(null);
        dto.setFechaExpiracion(fechaExp);
        return dto;
    }
}
