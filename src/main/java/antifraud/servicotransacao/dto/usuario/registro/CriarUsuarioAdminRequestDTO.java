package antifraud.servicotransacao.dto.usuario.registro;

import antifraud.servicotransacao.enums.PerfilUsuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarUsuarioAdminRequestDTO(

        @NotBlank(message = "O nome é obrigatório")
        @Schema(
                description = "Nome do usuário que será cadastrado",
                example = "João Silva"
        )
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Schema(
                description = "E-mail do usuário que será cadastrado",
                example = "joao@email.com"
        )
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Schema(
                description = "Senha inicial do usuário",
                example = "senha123"
        )
        String senha,

        @NotNull(message = "O perfil é obrigatório")
        @Schema(
                description = "Perfil de acesso atribuído ao usuário",
                example = "USUARIO"
        )
        PerfilUsuario perfil
) { }
