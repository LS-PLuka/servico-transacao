package antifraud.servicotransacao.service;

import antifraud.servicotransacao.dto.usuario.registro.CriarUsuarioAdminRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroResponseDTO;
import antifraud.servicotransacao.entity.Usuario;
import antifraud.servicotransacao.enums.PerfilUsuario;
import antifraud.servicotransacao.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroResponseDTO registrarUsuario(RegistroRequestDTO registroRequestDTO) {
        if (usuarioRepository.existsByEmail(registroRequestDTO.email())) {
            //falta implementar a excecao personalizada
            throw new IllegalArgumentException("E-mail já registrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(registroRequestDTO.nome())
                .email(registroRequestDTO.email())
                .senha(passwordEncoder.encode(registroRequestDTO.senha()))
                .perfil(PerfilUsuario.USUARIO)
                .criadoEm(LocalDateTime.now())
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        return toResponseDTO(usuarioSalvo);
    }

    public RegistroResponseDTO registrarUsuarioPorAdmin(CriarUsuarioAdminRequestDTO criarUsuarioAdminRequestDTO) {
        if (usuarioRepository.existsByEmail(criarUsuarioAdminRequestDTO.email())) {
            //falta implementar a excecao personalizada
            throw new IllegalArgumentException("E-mail já registrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(criarUsuarioAdminRequestDTO.nome())
                .email(criarUsuarioAdminRequestDTO.email())
                .senha(passwordEncoder.encode(criarUsuarioAdminRequestDTO.senha()))
                .perfil(criarUsuarioAdminRequestDTO.perfil())
                .criadoEm(LocalDateTime.now())
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        return toResponseDTO(usuarioSalvo);
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
