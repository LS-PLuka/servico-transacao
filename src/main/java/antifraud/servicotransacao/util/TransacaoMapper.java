package antifraud.servicotransacao.util;

import antifraud.servicotransacao.dto.transacao.TransacaoEventoDTO;
import antifraud.servicotransacao.dto.transacao.TransacaoRequestDTO;
import antifraud.servicotransacao.dto.transacao.TransacaoResponseDTO;
import antifraud.servicotransacao.entity.Transacao;
import antifraud.servicotransacao.enums.StatusTransacao;

import java.time.LocalDateTime;

public class TransacaoMapper {

    private TransacaoMapper() {
    }

    public static Transacao toEntity(TransacaoRequestDTO requestDTO) {
        return Transacao.builder()
                .contaId(requestDTO.contaId())
                .valor(requestDTO.valor())
                .categoria(requestDTO.categoria())
                .codigoPais(requestDTO.codigoPais())
                .status(StatusTransacao.PENDENTE)
                .criadoEm(LocalDateTime.now())
                .build();
    }

    public static TransacaoResponseDTO toResponseDTO(Transacao transacao) {
        return new TransacaoResponseDTO(
                transacao.getId(),
                transacao.getContaId(),
                transacao.getValor(),
                transacao.getCategoria(),
                transacao.getCodigoPais(),
                transacao.getStatus(),
                transacao.getCriadoEm()
        );
    }

    public static TransacaoEventoDTO toEventoDTO(Transacao transacao) {
        return new TransacaoEventoDTO(
                transacao.getId(),
                transacao.getContaId(),
                transacao.getValor(),
                transacao.getCategoria(),
                transacao.getCodigoPais(),
                transacao.getCriadoEm()
        );
    }
}
