package antifraud.servicotransacao.dto.transacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransacaoEventoDTO(
    UUID transacaoId,
    UUID contaId,
    BigDecimal valor,
    String categoria,
    String codigoPais,
    LocalDateTime dataHora
) { }
