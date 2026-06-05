package antifraud.servicotransacao.repository;

import antifraud.servicotransacao.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findById(UUID id);

    Page<Usuario> findAll(Pageable pageable);

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
