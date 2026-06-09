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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AutenticacaoUsuarioServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AutenticacaoUsuarioService autenticacaoUsuarioService;

    private RegistroRequestDTO registroRequest;
    private LoginRequestDTO loginRequest;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        registroRequest = new RegistroRequestDTO(
                "Teste",
                "teste@teste.com",
                "senha123"
        );

        loginRequest = new LoginRequestDTO(
                "teste@teste.com",
                "senha123"
        );

        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Teste")
                .email("teste@teste.com")
                .senha("senhaCriptografada")
                .perfil(PerfilUsuario.USUARIO)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Deve registrar usuario com sucesso quando os dados sao validos")
    void registrarUsuario_dadosValidos_deveRetornarDTO() {
        when(usuarioRepository.existsByEmail(registroRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registroRequest.senha())).thenReturn("senhaCriptografada");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        RegistroResponseDTO response = autenticacaoUsuarioService.registrarUsuario(registroRequest);

        assertNotNull(response);

        assertEquals(usuario.getId(), response.id());
        assertEquals(usuario.getNome(), response.nome());
        assertEquals(usuario.getEmail(), response.email());

        verify(usuarioRepository).existsByEmail(registroRequest.email());
        verify(passwordEncoder).encode(registroRequest.senha());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lancar EmailJaCadastradoException quando email ja existe")
    void registrarUsuario_emailDuplicado_deveLancarExcecao() {
        when(usuarioRepository.existsByEmail(registroRequest.email())).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class,
                () -> autenticacaoUsuarioService.registrarUsuario(registroRequest)
        );

        verify(usuarioRepository).existsByEmail(registroRequest.email());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve autenticar usuario e retornar token e perfil")
    void autenticarUsuario_credenciaisValidas_deveRetornarToken() {
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(authentication.getPrincipal()).thenReturn(usuario);

        when(jwtService.gerarToken(usuario)).thenReturn("jwt-token-valido");

        LoginResponseDTO response = autenticacaoUsuarioService.autenticarUsuario(loginRequest);

        assertNotNull(response);

        assertEquals("jwt-token-valido", response.token());
        assertEquals("Bearer", response.tipo());
        assertEquals("USUARIO", response.perfil());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).gerarToken(usuario);
    }

    @Test
    @DisplayName("Deve lancar excecao quando credenciais sao invalidas")
    void autenticarUsuario_credenciaisInvalidas_deveLancarExcecao() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciais invalidas"));

        assertThrows(BadCredentialsException.class,
                () -> autenticacaoUsuarioService.autenticarUsuario(loginRequest)
        );

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).gerarToken(any());
    }
}
