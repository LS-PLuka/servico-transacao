package antifraud.servicotransacao.service;

import antifraud.servicotransacao.dto.transacao.TransacaoRequestDTO;
import antifraud.servicotransacao.dto.transacao.TransacaoResponseDTO;
import antifraud.servicotransacao.entity.Transacao;
import antifraud.servicotransacao.exception.TransacaoNaoEncontradaException;
import antifraud.servicotransacao.messaging.TransacaoPublisher;
import antifraud.servicotransacao.repository.TransacaoRepository;
import antifraud.servicotransacao.util.TransacaoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static antifraud.servicotransacao.util.TransacaoMapper.toResponseDTO;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransacaoService {

    private final TransacaoRepository transacaoRepository;
    private final TransacaoPublisher transacaoPublisher;

    // garante rollback caso ocorra qualquer exceçao durante o processamento,
    // incluindo falhas ao publicar o evento no RabbitMQ
    @Transactional
    public TransacaoResponseDTO registrarTransacao(TransacaoRequestDTO requestDTO) {
        Transacao transacao = TransacaoMapper.toEntity(requestDTO);

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
}
