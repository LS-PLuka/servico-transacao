package antifraud.servicotransacao.dto.usuario.registro;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistroRequestDTO(

        @NotBlank(message = "O nome é obrigatório")
        @Schema(
                description = "Nome do usuário",
                example = "João Silva"
        )
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Schema(
                description = "E-mail utilizado para criar a conta",
                example = "joao@email.com"
        )
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Schema(
                description = "Senha utilizada para criar a conta",
                example = "senha123"
        )
        String senha
) { }
