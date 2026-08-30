package antifraud.servicotransacao.service;

import antifraud.servicotransacao.dto.transacao.TransacaoRequestDTO;
import antifraud.servicotransacao.dto.transacao.TransacaoResponseDTO;
import antifraud.servicotransacao.entity.Transacao;
import antifraud.servicotransacao.entity.Usuario;
import antifraud.servicotransacao.exception.AcessoNegadoException;
import antifraud.servicotransacao.exception.TransacaoNaoEncontradaException;
import antifraud.servicotransacao.exception.UsuarioNaoEncontradoException;
import antifraud.servicotransacao.messaging.TransacaoPublisher;
import antifraud.servicotransacao.repository.TransacaoRepository;
import antifraud.servicotransacao.repository.UsuarioRepository;
import antifraud.servicotransacao.util.PaginaResponseDTO;
import antifraud.servicotransacao.util.PaginaResponseMapper;
import antifraud.servicotransacao.util.TransacaoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static antifraud.servicotransacao.util.TransacaoMapper.toResponseDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoPublisher transacaoPublisher;
    private final UsuarioRepository usuarioRepository;

    // garante rollback caso ocorra qualquer exceçao durante o processamento,
    // incluindo falhas ao publicar o evento no RabbitMQ
    @Transactional
    public TransacaoResponseDTO registrarTransacao(TransacaoRequestDTO requestDTO) {
        // busca usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();

        // verifica se é ADMIN
        boolean isAdmin = usuarioLogado.getPerfil().name().equals("ADMIN");
        if (isAdmin) {
            log.warn("Tentativa de registro de transação por usuário ADMIN: UsuarioLogado={}", usuarioLogado.getId());
            throw new AcessoNegadoException("Usuários com perfil ADMIN não podem registrar transações");
        }

        Transacao transacao = TransacaoMapper.toEntity(requestDTO);

        // registra a transacao
        Transacao transacaoSalva = transacaoRepository.save(transacao);
        transacaoPublisher.publicarTransacao(TransacaoMapper.toEventoDTO(transacaoSalva));

        log.info("Transação registrada: ID={}, Conta={}",
                transacaoSalva.getId(),
                transacaoSalva.getContaId());

        return toResponseDTO(transacaoSalva);
    }

    @Transactional(readOnly = true)
    public TransacaoResponseDTO buscarTransacaoPorId(UUID id) {
        Transacao transacao = transacaoRepository.findById(id)
                .orElseThrow(() -> new TransacaoNaoEncontradaException("Transação não encontrada"));

        log.info("Transação encontrada: ID={}, Conta={}", transacao.getId(), transacao.getContaId());
        return toResponseDTO(transacao);
    }

    @Transactional(readOnly = true)
    public PaginaResponseDTO<TransacaoResponseDTO> listarTransacoesDeUmaConta(UUID contaId, String token, int pagina) {
        // busca usuario autenticado
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();

        Usuario usuario = usuarioRepository.findById(contaId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado"));

        // verifica tipo de perfil
        boolean isAdmin = usuarioLogado.getPerfil().name().equals("ADMIN");
        boolean isContaDoUsuario = usuario.getId().equals(usuarioLogado.getId());

        // se o usuario logado nao for ADMIN e contaId nao for dele,
        // ele nao tem permissao para acessar
        if (contaId != usuarioLogado.getId() && !isAdmin) {
            log.warn("Tentativa de acesso não autorizado às transações de outra conta: Conta={}, UsuarioLogado={}", contaId, usuarioLogado.getId());
            throw new AcessoNegadoException("Você não tem permissão para acessar as transações de outra conta");
        }

        // busca e retorna as transacoes
        Pageable pageable = PageRequest.of(pagina, 10);
        Page<Transacao> transacoesPage = transacaoRepository.findByContaId(contaId, pageable);

        log.info("Listando transações para a conta: Conta={}, Pagina={}, TotalTransacoes={}", contaId, pagina, transacoesPage.getTotalElements());
        return PaginaResponseMapper.fromPage(transacoesPage.map(transacao -> toResponseDTO(transacao)));
    }
}
