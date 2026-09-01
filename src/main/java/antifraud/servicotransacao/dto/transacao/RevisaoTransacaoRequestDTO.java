package antifraud.servicotransacao.dto.transacao;

import antifraud.servicotransacao.enums.StatusTransacao;
import jakarta.validation.constraints.NotNull;

public record RevisaoTransacaoRequestDTO(

    @NotNull(message = "O status é obrigatório")
    StatusTransacao status
) { }
