package antifraud.servicotransacao.dto.transacao;

import antifraud.servicotransacao.enums.StatusTransacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record RevisaoTransacaoRequestDTO(

        @NotNull(message = "O status é obrigatório")
        @Schema(
                description = "Novo status atribuído à transação durante a revisão",
                example = "APROVADA"
        )
        StatusTransacao status
) { }
