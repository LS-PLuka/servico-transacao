package antifraud.servicotransacao.dto.usuario.login;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Schema(
                description = "E-mail utilizado para autenticação",
                example = "usuario@email.com"
        )
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Schema(
                description = "Senha utilizada para autenticação",
                example = "senha123"
        )
        String senha
) { }
