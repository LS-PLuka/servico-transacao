package antifraud.servicotransacao.dto.transacao;

import antifraud.servicotransacao.enums.StatusTransacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransacaoResponseDTO(
        UUID id,
        UUID contaId,
        BigDecimal valor,
        String categoria,
        String codigoPais,
        StatusTransacao status,
        LocalDateTime criadoEm
) { }
