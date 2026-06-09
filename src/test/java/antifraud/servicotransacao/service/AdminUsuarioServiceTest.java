package antifraud.servicotransacao.service;

import antifraud.servicotransacao.dto.usuario.registro.CriarUsuarioAdminRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroResponseDTO;
import antifraud.servicotransacao.entity.Usuario;
import antifraud.servicotransacao.enums.PerfilUsuario;
import antifraud.servicotransacao.exception.EmailJaCadastradoException;
import antifraud.servicotransacao.exception.UsuarioNaoEncontradoException;
import antifraud.servicotransacao.repository.UsuarioRepository;
import antifraud.servicotransacao.util.PaginaResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminUsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUsuarioService adminUsuarioService;

    private Usuario usuario;
    private CriarUsuarioAdminRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new CriarUsuarioAdminRequestDTO(
                "Teste",
                "teste@teste.com",
                "123456",
                PerfilUsuario.USUARIO
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
    void registrarUsuarioPorAdmin_dadosValidos_deveRetornarDTO() {
        when(usuarioRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.senha())).thenReturn("senhaCriptografada");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario);

        RegistroResponseDTO response = adminUsuarioService.registrarUsuarioPorAdmin(request);

        assertNotNull(response);

        assertEquals(usuario.getId(), response.id());
        assertEquals(usuario.getNome(), response.nome());
        assertEquals(usuario.getEmail(), response.email());

        verify(usuarioRepository).existsByEmail(request.email());
        verify(passwordEncoder).encode(request.senha());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve registrar usuario com sucesso quando os dados sao validos")
    void registrarUsuarioPorAdmin_emailDuplicado_deveRetornarExcecao() {
        when(usuarioRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EmailJaCadastradoException.class, () -> adminUsuarioService.registrarUsuarioPorAdmin(request));

        verify(usuarioRepository).existsByEmail(request.email());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar DTO quando usuario é encontrado por ID")
    void buscarUsuarioPorId_idExistente_deveRetornarDTO() {
        UUID id = usuario.getId();
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));

        RegistroResponseDTO response = adminUsuarioService.buscarUsuarioPorId(id);

        assertNotNull(response);

        assertEquals(usuario.getId(), response.id());
        assertEquals(usuario.getNome(), response.nome());
        assertEquals(usuario.getEmail(), response.email());

        verify(usuarioRepository).findById(id);
    }

    @Test
    @DisplayName("Deve lançar UsuarioNaoEncontradoException quando ID nao existe")
    void buscarUsuarioPorId_idInexistente_deveLancarExcecao() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UsuarioNaoEncontradoException.class, () -> adminUsuarioService.buscarUsuarioPorId(id));

        verify(usuarioRepository).findById(id);
    }

    @Test
    @DisplayName("Deve retornar pagina com usuarios quando existem registros")
    void listarTodosUsuarios_comUsuarios_deveRetornarPagina() {
        Page<Usuario> pagina = new PageImpl<>(List.of(usuario));
        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(pagina);

        PaginaResponseDTO<RegistroResponseDTO> response = adminUsuarioService.listarTodosUsuarios(0);

        assertNotNull(response);

        assertEquals(1, response.totalItens());
        assertEquals(1, response.conteudo().size());

        RegistroResponseDTO usuarioResponse = response.conteudo().get(0);

        assertEquals(usuario.getId(), usuarioResponse.id());
        assertEquals(usuario.getNome(), usuarioResponse.nome());
        assertEquals(usuario.getEmail(), usuarioResponse.email());

        verify(usuarioRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Deve retornar pagina vazia quando não tem usuarios")
    void listarTodosUsuarios_semUsuarios_deveRetornarPaginaVazia() {
        Page<Usuario> paginaVazia = Page.empty();
        when(usuarioRepository.findAll(any(Pageable.class))).thenReturn(paginaVazia);

        PaginaResponseDTO<RegistroResponseDTO> response = adminUsuarioService.listarTodosUsuarios(0);

        assertNotNull(response);

        assertEquals(0, response.totalItens());
        assertTrue(response.conteudo().isEmpty());

        verify(usuarioRepository).findAll(any(Pageable.class));
    }
}
