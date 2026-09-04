package antifraud.servicotransacao.dto.transacao;

import antifraud.servicotransacao.enums.StatusTransacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransacaoResponseDTO(

        @Schema(
                description = "Identificador único da transação",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Identificador da conta responsável pela transação",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID contaId,

        @Schema(
                description = "Valor monetário da transação",
                example = "150.75"
        )
        BigDecimal valor,

        @Schema(
                description = "Categoria do estabelecimento onde a transação foi realizada",
                example = "RESTAURANTE"
        )
        String categoria,

        @Schema(
                description = "Código do país onde a transação foi realizada",
                example = "BRA"
        )
        String codigoPais,

        @Schema(
                description = "Status atual da transação",
                example = "PENDENTE"
        )
        StatusTransacao status,

        @Schema(
                description = "Data e hora em que a transação foi registrada no sistema",
                example = "2026-09-04T13:30:00"
        )
        LocalDateTime criadoEm
) { }
