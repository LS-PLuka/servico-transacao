package antifraud.servicotransacao.dto.transacao;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransacaoRequestDTO(

        @NotNull(message = "O identificador da conta é obrigatório")
        @Schema(
                description = "Identificador da conta que realizará a transação",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID contaId,

        @NotNull(message = "O valor da transação é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor da transação deve ser maior que zero")
        @Schema(
                description = "Valor monetário da transação",
                example = "150.75"
        )
        BigDecimal valor,

        @NotBlank(message = "A categoria da transação é obrigatória")
        @Schema(
                description = "Categoria do estabelecimento onde a transação foi realizada",
                example = "RESTAURANTE"
        )
        String categoria,

        @NotBlank(message = "O código do país é obrigatório")
        @Size(min = 3, max = 3, message = "O código do país deve ter exatamente 3 letras (ex: BRA, USA, ARG)")
        @Pattern(regexp = "^[A-Z]{3}$", message = "O código do país deve conter apenas letras maiúsculas (ex: BRA, USA, ARG)")
        @Schema(
                description = "Código do país onde a transação foi realizada",
                example = "BRA",
                minLength = 3,
                maxLength = 3
        )
        String codigoPais,

        @NotNull(message = "A data e hora são obrigatórias")
        @Schema(
                description = "Data e hora em que a transação foi realizada",
                example = "2026-09-04T13:30:00"
        )
        LocalDateTime dataHora
) { }
