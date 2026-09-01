package antifraud.servicotransacao.service;

import antifraud.servicotransacao.dto.transacao.TransacaoRequestDTO;
import antifraud.servicotransacao.dto.transacao.TransacaoResponseDTO;
import antifraud.servicotransacao.entity.Transacao;
import antifraud.servicotransacao.entity.Usuario;
import antifraud.servicotransacao.enums.PerfilUsuario;
import antifraud.servicotransacao.enums.StatusTransacao;
import antifraud.servicotransacao.exception.AcessoNegadoException;
import antifraud.servicotransacao.exception.TransacaoNaoEncontradaException;
import antifraud.servicotransacao.exception.UsuarioNaoEncontradoException;
import antifraud.servicotransacao.messaging.TransacaoPublisher;
import antifraud.servicotransacao.repository.TransacaoRepository;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransacaoServiceTest {

    @Mock
    private TransacaoRepository transacaoRepository;

    @Mock
    private TransacaoPublisher transacaoPublisher;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TransacaoService transacaoService;

    private Usuario usuario;
    private Usuario admin;
    private Transacao transacao;
    private TransacaoRequestDTO request;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Teste")
                .email("teste@teste.com")
                .senha("senhaCriptografada")
                .perfil(PerfilUsuario.USUARIO)
                .criadoEm(LocalDateTime.now())
                .build();

        admin = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Admin")
                .email("admin@teste.com")
                .senha("senhaCriptografada")
                .perfil(PerfilUsuario.ADMIN)
                .criadoEm(LocalDateTime.now())
                .build();

        request = new TransacaoRequestDTO(
                usuario.getId(),
                new BigDecimal("100.00"),
                "ALIMENTACAO",
                "BRA",
                LocalDateTime.now()
        );

        transacao = Transacao.builder()
                .id(UUID.randomUUID())
                .contaId(usuario.getId())
                .valor(new BigDecimal("100.00"))
                .categoria("ALIMENTACAO")
                .codigoPais("BRA")
                .status(StatusTransacao.PENDENTE)
                .criadoEm(request.dataHora())
                .build();
    }

    @Test
    @DisplayName("Deve registrar transacao com sucesso quando usuario e dono da conta")
    void efetuarTransacao_usuarioDaConta_deveRegistrarTransacao() {
        // Arrange
        autenticarUsuario(usuario);

        when(transacaoRepository.save(any(Transacao.class))).thenReturn(transacao);

        // Act
        TransacaoResponseDTO response = transacaoService.efetuarTransacao(request);

        // Assert
        assertNotNull(response);

        assertEquals(transacao.getId(), response.id());
        assertEquals(transacao.getContaId(), response.contaId());
        assertEquals(transacao.getValor(), response.valor());
        assertEquals(transacao.getCategoria(), response.categoria());
        assertEquals(transacao.getCodigoPais(), response.codigoPais());
        assertEquals(transacao.getStatus(), response.status());

        verify(transacaoRepository).save(any(Transacao.class));
        verify(transacaoPublisher).publicarTransacao(any());
    }

    @Test
    @DisplayName("Deve negar registro de transacao quando usuario e ADMIN")
    void efetuarTransacao_usuarioAdmin_deveLancarExcecao() {
        // Arrange
        autenticarUsuario(admin);

        // Act + Assert
        assertThrows(
                AcessoNegadoException.class,
                () -> transacaoService.efetuarTransacao(request)
        );

        verify(transacaoRepository, never()).save(any());
        verify(transacaoPublisher, never()).publicarTransacao(any());
    }

    @Test
    @DisplayName("Deve negar registro de transacao para outra conta")
    void efetuarTransacao_outraConta_deveLancarExcecao() {
        // Arrange
        autenticarUsuario(usuario);

        TransacaoRequestDTO requestOutraConta = new TransacaoRequestDTO(
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "ALIMENTACAO",
                "BRA",
                LocalDateTime.now()
        );

        // Act + Assert
        assertThrows(
                AcessoNegadoException.class,
                () -> transacaoService.efetuarTransacao(requestOutraConta)
        );

        verify(transacaoRepository, never()).save(any());
        verify(transacaoPublisher, never()).publicarTransacao(any());
    }

    @Test
    @DisplayName("Deve retornar transacao quando usuario tem permissao")
    void buscarTransacaoPorId_usuarioComPermissao_deveRetornarDTO() {
        // Arrange
        autenticarUsuario(usuario);

        UUID contaId = usuario.getId();
        UUID transacaoId = transacao.getId();

        when(usuarioRepository.findById(contaId)).thenReturn(Optional.of(usuario));
        when(transacaoRepository.findById(transacaoId)).thenReturn(Optional.of(transacao));

        // Act
        TransacaoResponseDTO response =
                transacaoService.buscarTransacaoPorId(transacaoId, contaId);

        // Assert
        assertNotNull(response);

        assertEquals(transacao.getId(), response.id());
        assertEquals(transacao.getContaId(), response.contaId());
        assertEquals(transacao.getValor(), response.valor());
        assertEquals(transacao.getCategoria(), response.categoria());

        verify(usuarioRepository).findById(contaId);
        verify(transacaoRepository).findById(transacaoId);
    }

    @Test
    @DisplayName("Deve negar acesso quando usuario tenta consultar transacao de outra conta")
    void buscarTransacaoPorId_outraConta_deveLancarExcecao() {
        // Arrange
        autenticarUsuario(usuario);

        UUID outraContaId = UUID.randomUUID();

        when(usuarioRepository.findById(outraContaId)).thenReturn(Optional.of(
                Usuario.builder()
                        .id(outraContaId)
                        .nome("Outro Usuario")
                        .email("outro@teste.com")
                        .perfil(PerfilUsuario.USUARIO)
                        .build()
        ));

        // Act + Assert
        assertThrows(
                AcessoNegadoException.class,
                () -> transacaoService.buscarTransacaoPorId(
                        transacao.getId(),
                        outraContaId
                )
        );

        verify(transacaoRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Deve lançar UsuarioNaoEncontradoException quando conta nao existe")
    void buscarTransacaoPorId_contaInexistente_deveLancarExcecao() {
        // Arrange
        autenticarUsuario(usuario);

        UUID contaId = UUID.randomUUID();

        when(usuarioRepository.findById(contaId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                UsuarioNaoEncontradoException.class,
                () -> transacaoService.buscarTransacaoPorId(
                        transacao.getId(),
                        contaId
                )
        );

        verify(transacaoRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Deve lançar TransacaoNaoEncontradaException quando transacao nao existe")
    void buscarTransacaoPorId_transacaoInexistente_deveLancarExcecao() {
        // Arrange
        autenticarUsuario(usuario);

        UUID contaId = usuario.getId();
        UUID transacaoId = UUID.randomUUID();

        when(usuarioRepository.findById(contaId)).thenReturn(Optional.of(usuario));
        when(transacaoRepository.findById(transacaoId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                TransacaoNaoEncontradaException.class,
                () -> transacaoService.buscarTransacaoPorId(
                        transacaoId,
                        contaId
                )
        );

        verify(usuarioRepository).findById(contaId);
        verify(transacaoRepository).findById(transacaoId);
    }

    @Test
    @DisplayName("Deve permitir que ADMIN consulte transacao de outra conta")
    void buscarTransacaoPorId_admin_deveRetornarDTO() {
        // Arrange
        autenticarUsuario(admin);

        UUID contaId = usuario.getId();
        UUID transacaoId = transacao.getId();

        when(usuarioRepository.findById(contaId)).thenReturn(Optional.of(usuario));
        when(transacaoRepository.findById(transacaoId)).thenReturn(Optional.of(transacao));

        // Act
        TransacaoResponseDTO response =
                transacaoService.buscarTransacaoPorId(transacaoId, contaId);

        // Assert
        assertNotNull(response);
        assertEquals(transacao.getId(), response.id());
        assertEquals(transacao.getContaId(), response.contaId());

        verify(usuarioRepository).findById(contaId);
        verify(transacaoRepository).findById(transacaoId);
    }

    @Test
    @DisplayName("Deve retornar pagina com transacoes quando existem registros")
    void listarTransacoesDeUmaConta_comTransacoes_deveRetornarPagina() {
        // Arrange
        autenticarUsuario(usuario);

        UUID contaId = usuario.getId();

        when(usuarioRepository.findById(contaId)).thenReturn(Optional.of(usuario));

        Page<Transacao> pagina = new PageImpl<>(List.of(transacao));

        when(transacaoRepository.findByContaId(
                eq(contaId),
                any(Pageable.class)
        )).thenReturn(pagina);

        // Act
        PaginaResponseDTO<TransacaoResponseDTO> response =
                transacaoService.listarTransacoesDeUmaConta(contaId, 0);

        // Assert
        assertNotNull(response);

        assertEquals(1, response.totalItens());
        assertEquals(1, response.conteudo().size());

        TransacaoResponseDTO transacaoResponse = response.conteudo().get(0);

        assertEquals(transacao.getId(), transacaoResponse.id());
        assertEquals(transacao.getContaId(), transacaoResponse.contaId());
        assertEquals(transacao.getValor(), transacaoResponse.valor());

        verify(usuarioRepository).findById(contaId);
        verify(transacaoRepository).findByContaId(
                eq(contaId),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("Deve retornar pagina vazia quando nao existem transacoes")
    void listarTransacoesDeUmaConta_semTransacoes_deveRetornarPaginaVazia() {
        // Arrange
        autenticarUsuario(usuario);

        UUID contaId = usuario.getId();

        when(usuarioRepository.findById(contaId)).thenReturn(Optional.of(usuario));

        Page<Transacao> paginaVazia = Page.empty();

        when(transacaoRepository.findByContaId(
                eq(contaId),
                any(Pageable.class)
        )).thenReturn(paginaVazia);

        // Act
        PaginaResponseDTO<TransacaoResponseDTO> response =
                transacaoService.listarTransacoesDeUmaConta(contaId, 0);

        // Assert
        assertNotNull(response);

        assertEquals(0, response.totalItens());
        assertTrue(response.conteudo().isEmpty());

        verify(usuarioRepository).findById(contaId);
        verify(transacaoRepository).findByContaId(
                eq(contaId),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("Deve negar listagem de transacoes de outra conta")
    void listarTransacoesDeUmaConta_outraConta_deveLancarExcecao() {
        // Arrange
        autenticarUsuario(usuario);

        UUID outraContaId = UUID.randomUUID();

        Usuario outroUsuario = Usuario.builder()
                .id(outraContaId)
                .nome("Outro Usuario")
                .email("outro@teste.com")
                .perfil(PerfilUsuario.USUARIO)
                .build();

        when(usuarioRepository.findById(outraContaId))
                .thenReturn(Optional.of(outroUsuario));

        // Act + Assert
        assertThrows(
                AcessoNegadoException.class,
                () -> transacaoService.listarTransacoesDeUmaConta(
                        outraContaId,
                        0
                )
        );

        verify(transacaoRepository, never())
                .findByContaId(any(UUID.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Deve permitir que ADMIN liste transacoes de outra conta")
    void listarTransacoesDeUmaConta_admin_deveRetornarPagina() {
        // Arrange
        autenticarUsuario(admin);

        UUID contaId = usuario.getId();

        when(usuarioRepository.findById(contaId)).thenReturn(Optional.of(usuario));

        Page<Transacao> pagina = new PageImpl<>(List.of(transacao));

        when(transacaoRepository.findByContaId(
                eq(contaId),
                any(Pageable.class)
        )).thenReturn(pagina);

        // Act
        PaginaResponseDTO<TransacaoResponseDTO> response =
                transacaoService.listarTransacoesDeUmaConta(contaId, 0);

        // Assert
        assertNotNull(response);

        assertEquals(1, response.totalItens());
        assertEquals(1, response.conteudo().size());

        verify(usuarioRepository).findById(contaId);
        verify(transacaoRepository).findByContaId(
                eq(contaId),
                any(Pageable.class)
        );
    }

    private void autenticarUsuario(Usuario usuario) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        usuario.getAuthorities()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
