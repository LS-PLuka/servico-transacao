package antifraud.servicotransacao.service;

import antifraud.servicotransacao.dto.usuario.login.LoginRequestDTO;
import antifraud.servicotransacao.dto.usuario.login.LoginResponseDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroResponseDTO;
import antifraud.servicotransacao.entity.Usuario;
import antifraud.servicotransacao.enums.PerfilUsuario;
import antifraud.servicotransacao.exception.EmailJaCadastradoException;
import antifraud.servicotransacao.repository.UsuarioRepository;
import antifraud.servicotransacao.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutenticacaoUsuarioService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegistroResponseDTO registrarUsuario(RegistroRequestDTO registroRequestDTO) {
        if (usuarioRepository.existsByEmail(registroRequestDTO.email())) {
            log.warn("Tentativa de registro com email já cadastrado: {}", registroRequestDTO.email());
            throw new EmailJaCadastradoException("E-mail já registrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(registroRequestDTO.nome())
                .email(registroRequestDTO.email())
                .senha(passwordEncoder.encode(registroRequestDTO.senha()))
                .perfil(PerfilUsuario.USUARIO)
                .criadoEm(LocalDateTime.now())
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        log.info("Novo usuario registrado: {}", usuarioSalvo.getEmail());
        return toResponseDTO(usuarioSalvo);
    }

    public LoginResponseDTO autenticarUsuario(LoginRequestDTO loginRequest) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.senha());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        Usuario usuario = (Usuario) authentication.getPrincipal();
        String token = jwtService.gerarToken(usuario);

        log.info("Usuario autenticado: Email={}, Perfil={}", usuario.getEmail(), usuario.getPerfil());
        return new LoginResponseDTO(token, "Bearer", usuario.getPerfil().toString());
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
