package antifraud.servicotransacao.dto.transacao;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransacaoRequestDTO(

    @NotNull(message = "O identificador da conta é obrigatório")
    UUID contaId,

    @NotNull(message = "O valor da transação é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor da transação deve ser maior que zero")
    BigDecimal valor,

    @NotBlank(message = "A categoria da transação é obrigatória")
    String categoria,

    @NotBlank(message = "O código do país é obrigatório")
    @Size(min = 3, max = 3, message = "O código do país deve ter exatamente 3 letras (ex: BRA, USA, ARG)")
    @Pattern(regexp = "^[A-Z]{3}$", message = "O código do país deve conter apenas letras maiúsculas (ex: BRA, USA, ARG)")
    String codigoPais,

    @NotNull(message = "A data e hora são obrigatórias")
    LocalDateTime dataHora
) { }
