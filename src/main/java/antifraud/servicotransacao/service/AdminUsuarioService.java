package antifraud.servicotransacao.service;

import antifraud.servicotransacao.dto.usuario.registro.CriarUsuarioAdminRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroResponseDTO;
import antifraud.servicotransacao.entity.Usuario;
import antifraud.servicotransacao.exception.EmailJaCadastradoException;
import antifraud.servicotransacao.exception.UsuarioNaoEncontradoException;
import antifraud.servicotransacao.repository.UsuarioRepository;
import antifraud.servicotransacao.util.PaginaResponseDTO;
import antifraud.servicotransacao.util.PaginaResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    //usuario ADMIN pode registrar outros usuarios, inclusive outros admins
    public RegistroResponseDTO registrarUsuarioPorAdmin(CriarUsuarioAdminRequestDTO criarUsuarioAdminRequestDTO) {
        if (usuarioRepository.existsByEmail(criarUsuarioAdminRequestDTO.email())) {
            log.warn("Tentativa de registro de usuario por admin com email já registrado: {}", criarUsuarioAdminRequestDTO.email());
            throw new EmailJaCadastradoException("E-mail já registrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(criarUsuarioAdminRequestDTO.nome())
                .email(criarUsuarioAdminRequestDTO.email())
                .senha(passwordEncoder.encode(criarUsuarioAdminRequestDTO.senha()))
                .perfil(criarUsuarioAdminRequestDTO.perfil())
                .criadoEm(LocalDateTime.now())
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        log.info("Usuario registrado por admin: Email={}, Perfil={}", usuarioSalvo.getEmail(), usuarioSalvo.getPerfil());
        return toResponseDTO(usuarioSalvo);
    }

    public RegistroResponseDTO buscarUsuarioPorId(UUID id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        log.info("Usuario encontrado: Email={}", usuario.getEmail());
        return toResponseDTO(usuario);
    }

    public PaginaResponseDTO<RegistroResponseDTO> listarTodosUsuarios(int pagina) {
        Pageable pageable = PageRequest.of(pagina, 10);
        Page<Usuario> usuarios = usuarioRepository.findAll(pageable);

        log.info("Listando usuarios: Pagina={}, Total de usuarios={}", pagina, usuarios.getTotalElements());
        return PaginaResponseMapper.fromPage(usuarios.map(usuario -> toResponseDTO(usuario)));
    }

    //metodo auxiliar para converter Usuario em RegistroResponseDTO
    private RegistroResponseDTO toResponseDTO(Usuario usuario) {
        return new RegistroResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil().toString(),
                usuario.getCriadoEm()
        );
    }
}
