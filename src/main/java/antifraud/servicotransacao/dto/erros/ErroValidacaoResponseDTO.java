package antifraud.servicotransacao.dto.erros;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

public record ErroValidacaoResponseDTO(

        @Schema(
                description = "Código HTTP retornado pela API",
                example = "400"
        )
        int status,

        @Schema(
                description = "Tipo do erro ocorrido",
                example = "Erro de validação"
        )
        String erro,

        @Schema(
                description = "Erros de validação associados a cada campo da requisição",
                example = "{\"email\": \"E-mail inválido\", \"senha\": \"A senha deve possuir no mínimo 6 caracteres\"}"
        )
        Map<String, String> erros
) { }
