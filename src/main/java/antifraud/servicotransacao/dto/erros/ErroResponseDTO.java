package antifraud.servicotransacao.dto.erros;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErroResponseDTO(

        @Schema(
                description = "Código HTTP retornado pela API",
                example = "404"
        )
        int status,

        @Schema(
                description = "Tipo do erro ocorrido",
                example = "Recurso não encontrado"
        )
        String erro,

        @Schema(
                description = "Mensagem detalhando o erro ocorrido",
                example = "Usuário não encontrado"
        )
        String mensagem
) { }
