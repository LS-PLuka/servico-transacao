package antifraud.servicotransacao.dto.usuario.login;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDTO(

        @Schema(
                description = "Token JWT utilizado para autenticar as requisições",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c3VhcmlvQGVtYWlsLmNvbSJ9..."
        )
        String token,

        @Schema(
                description = "Tipo de autenticação utilizado pelo token",
                example = "Bearer"
        )
        String tipo,

        @Schema(
                description = "Perfil de acesso do usuário autenticado",
                example = "USUARIO"
        )
        String perfil
) { }
