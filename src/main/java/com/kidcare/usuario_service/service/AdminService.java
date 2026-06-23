package com.kidcare.usuario_service.service;

import com.kidcare.usuario_service.dto.*;
import com.kidcare.usuario_service.model.*;
import com.kidcare.usuario_service.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private AuditoriaRepository auditoriaRepository;
    @Autowired private AuditoriaService auditoriaService;
    @Autowired private InvitacionRepository invitacionRepository;
    @Autowired private UsuarioMenorRepository usuarioMenorRepository;
    @Autowired private MenorRepository menorRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public List<AdminUsuarioResponseDTO> listarUsuarios() {
        return usuarioRepository.findByEliminadoFalse().stream().map(this::mapToAdminDTO).collect(Collectors.toList());
    }

    public AdminUsuarioResponseDTO crearUsuario(CrearUsuarioAdminDTO dto, Integer idAdmin) {
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent())
            throw new RuntimeException("Ya existe un usuario con ese email");
        validarPassword(dto.getPassword());
        Rol rol = rolRepository.findById(dto.getIdRol()).orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setEmail(dto.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setFechaCreacion(LocalDate.now());
        usuarioRepository.save(usuario);
        auditoriaService.registrarAccion(idAdmin, "CREAR", "USUARIO", usuario.getIdUsuario());
        return mapToAdminDTO(usuario);
    }

    public AdminUsuarioResponseDTO editarUsuario(Integer idUsuario, EditarUsuarioAdminDTO dto, Integer idAdmin) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuarioRepository.findByEmail(dto.getEmail()).ifPresent(e -> {
            if (!e.getIdUsuario().equals(idUsuario)) throw new RuntimeException("El email ya esta en uso");
        });
        Rol rol = rolRepository.findById(dto.getIdRol())
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setEmail(dto.getEmail());
        usuario.setRol(rol);
        usuarioRepository.save(usuario);
        auditoriaService.registrarAccion(idAdmin, "EDITAR", "USUARIO", idUsuario);
        return mapToAdminDTO(usuario);
    }

    @Transactional
    public void eliminarUsuario(Integer idUsuario, Integer idAdmin) {
        if (idUsuario.equals(idAdmin)) throw new RuntimeException("No puedes eliminar tu propia cuenta");
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        invitacionRepository.deleteAll(invitacionRepository.findByIdTutor(idUsuario));
        List<UsuarioMenor> vinculos = usuarioMenorRepository.findByIdIdUsuario(idUsuario);
        List<Integer> menoresHuerfanos = new ArrayList<>();
        for (UsuarioMenor v : vinculos) {
            Integer idMenor = v.getMenor().getIdMenor();
            if (usuarioMenorRepository.findByIdIdMenorAndIdIdUsuarioNot(idMenor, idUsuario).isEmpty())
                menoresHuerfanos.add(idMenor);
        }
        invitacionRepository.deleteAll(invitacionRepository.findByIdMenorIn(menoresHuerfanos));
        usuarioMenorRepository.deleteAll(vinculos);
        menoresHuerfanos.forEach(menorRepository::deleteById);
        usuario.setActivo(false);
        usuario.setEliminado(true);
        usuarioRepository.save(usuario);
        auditoriaService.registrarAccion(idAdmin, "ELIMINAR", "USUARIO", idUsuario);
    }

    public void habilitarCuenta(Integer idUsuario, Integer idAdmin) {
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (u.getActivo()) throw new RuntimeException("La cuenta ya esta habilitada");
        u.setActivo(true);
        usuarioRepository.save(u);
        auditoriaService.registrarAccion(idAdmin, "HABILITAR", "USUARIO", idUsuario);
    }

    public void deshabilitarCuenta(Integer idUsuario, Integer idAdmin) {
        if (idUsuario.equals(idAdmin)) throw new RuntimeException("No puedes deshabilitar tu propia cuenta");
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!u.getActivo()) throw new RuntimeException("La cuenta ya esta deshabilitada");
        u.setActivo(false);
        usuarioRepository.save(u);
        auditoriaService.registrarAccion(idAdmin, "DESHABILITAR", "USUARIO", idUsuario);
    }

    public void asignarRol(Integer idUsuario, Integer idRol, Integer idAdmin) {
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        u.setRol(rol);
        usuarioRepository.save(u);
        auditoriaService.registrarAccion(idAdmin, "MODIFICAR_ROL", "USUARIO", idUsuario);
    }

    @Transactional
    public MenorResponseDTO crearMenorParaUsuario(Integer idUsuario, MenorRequestDTO dto, Integer idAdmin) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Menor menor = new Menor();
        menor.setNombre(dto.getNombre());
        menor.setFechaNacimiento(dto.getFechaNacimiento());
        menor.setSexo(dto.getSexo());
        menor.setEmoji(dto.getEmoji());
        menorRepository.save(menor);
        UsuarioMenorId pid = new UsuarioMenorId();
        pid.setIdUsuario(idUsuario);
        pid.setIdMenor(menor.getIdMenor());
        UsuarioMenor vinculo = new UsuarioMenor();
        vinculo.setId(pid);
        vinculo.setUsuario(usuario);
        vinculo.setMenor(menor);
        usuarioMenorRepository.save(vinculo);
        auditoriaService.registrarAccion(idAdmin, "CREAR", "MENOR", menor.getIdMenor());
        return mapToMenorDTO(menor);
    }

    public List<MenorResponseDTO> listarMenores() {
        return menorRepository.findAll().stream().map(this::mapToMenorDTO).collect(Collectors.toList());
    }

    public MenorResponseDTO editarMenor(Integer idMenor, MenorRequestDTO dto, Integer idAdmin) {
        Menor menor = menorRepository.findById(idMenor)
                .orElseThrow(() -> new RuntimeException("Menor no encontrado"));
        menor.setNombre(dto.getNombre());
        menor.setFechaNacimiento(dto.getFechaNacimiento());
        menor.setSexo(dto.getSexo());
        menor.setEmoji(dto.getEmoji());
        menorRepository.save(menor);
        auditoriaService.registrarAccion(idAdmin, "EDITAR", "MENOR", idMenor);
        return mapToMenorDTO(menor);
    }

    @Transactional
    public void eliminarMenor(Integer idMenor, Integer idAdmin) {
        if (!menorRepository.existsById(idMenor)) throw new RuntimeException("Menor no encontrado");
        usuarioMenorRepository.deleteAll(usuarioMenorRepository.findByIdIdMenor(idMenor));
        menorRepository.deleteById(idMenor);
        auditoriaService.registrarAccion(idAdmin, "ELIMINAR", "MENOR", idMenor);
    }

    public void asociarUsuarioMenor(Integer idMenor, Integer idUsuario, Integer idAdmin) {
        Menor menor = menorRepository.findById(idMenor)
                .orElseThrow(() -> new RuntimeException("Menor no encontrado"));
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        UsuarioMenorId pid = new UsuarioMenorId();
        pid.setIdUsuario(idUsuario);
        pid.setIdMenor(idMenor);
        if (usuarioMenorRepository.existsById(pid)) throw new RuntimeException("El usuario ya esta vinculado a este menor");
        UsuarioMenor v = new UsuarioMenor();
        v.setId(pid); v.setUsuario(usuario); v.setMenor(menor);
        usuarioMenorRepository.save(v);
        auditoriaService.registrarAccion(idAdmin, "VINCULAR", "USUARIO_MENOR", idMenor);
    }

    public List<AuditoriaResponseDTO> consultarAuditoria(String cambio, String entidad, LocalDate desde, LocalDate hasta) {
        List<Auditoria> r;
        if (desde != null && hasta != null) r = auditoriaRepository.findByFechaBetween(desde, hasta);
        else if (cambio != null && !cambio.isBlank()) r = auditoriaRepository.findByCambioContainingIgnoreCase(cambio);
        else if (entidad != null && !entidad.isBlank()) r = auditoriaRepository.findByEntidad(entidad);
        else r = auditoriaRepository.findAll();
        return r.stream().map(this::mapToAuditoriaDTO).collect(Collectors.toList());
    }

    // Misma política de contraseñas que el registro público: mínimo 8 caracteres, una mayúscula y un símbolo
    private void validarPassword(String password) {
        if (password == null || password.length() < 8)
            throw new RuntimeException("La contraseña debe tener al menos 8 caracteres");
        if (password.chars().noneMatch(Character::isUpperCase))
            throw new RuntimeException("La contraseña debe contener al menos una letra mayúscula");
        if (password.chars().allMatch(Character::isLetterOrDigit))
            throw new RuntimeException("La contraseña debe contener al menos un símbolo especial (!@#$%...)");
    }

    private AdminUsuarioResponseDTO mapToAdminDTO(Usuario u) {
        AdminUsuarioResponseDTO dto = new AdminUsuarioResponseDTO();
        dto.setIdUsuario(u.getIdUsuario()); dto.setNombreCompleto(u.getNombreCompleto());
        dto.setEmail(u.getEmail()); dto.setRol(u.getRol().getNombre());
        dto.setActivo(u.getActivo()); dto.setFechaCreacion(u.getFechaCreacion());
        return dto;
    }

    private MenorResponseDTO mapToMenorDTO(Menor m) {
        MenorResponseDTO dto = new MenorResponseDTO();
        dto.setIdMenor(m.getIdMenor()); dto.setNombre(m.getNombre());
        dto.setFechaNacimiento(m.getFechaNacimiento()); dto.setSexo(m.getSexo()); dto.setEmoji(m.getEmoji());
        return dto;
    }

    private AuditoriaResponseDTO mapToAuditoriaDTO(Auditoria a) {
        AuditoriaResponseDTO dto = new AuditoriaResponseDTO();
        dto.setIdAuditoria(a.getIdAuditoria()); dto.setEmailAdmin(a.getUsuario().getEmail());
        dto.setCambio(a.getCambio()); dto.setEntidad(a.getEntidad());
        dto.setIdEntidad(a.getIdEntidad()); dto.setFecha(a.getFecha());
        return dto;
    }
}
