package antifraud.servicotransacao.dto.usuario.registro;

import antifraud.servicotransacao.enums.PerfilUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarUsuarioAdminRequestDTO(

    @NotBlank(message = "O nome é obrigatório")
    String nome,

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "Informe um e-mail válido")
    String email,

    @NotBlank(message = "A senha é obrigatória")
    String senha,

    @NotNull(message = "O perfil é obrigatório")
    PerfilUsuario perfil
) { }
