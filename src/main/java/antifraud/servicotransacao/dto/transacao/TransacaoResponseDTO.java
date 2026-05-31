package antifraud.servicotransacao.dto.transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransacaoResponseDTO(
    UUID id,
    UUID contaId,
    BigDecimal valor,
    String categoria,
    String codigoPais,
    String Status,
    LocalDate criadoEm
) { }
