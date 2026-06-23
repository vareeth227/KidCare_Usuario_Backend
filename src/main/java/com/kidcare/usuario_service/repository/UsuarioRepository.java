package com.kidcare.usuario_service.repository;

import com.kidcare.usuario_service.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

// Repositorio que maneja el acceso a datos de la entidad Usuario
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);
    Boolean existsByEmail(String email);
    Optional<Usuario> findByTokenRecuperacion(String tokenRecuperacion);
    List<Usuario> findByEliminadoFalse();
}