package antifraud.servicotransacao.dto.usuario.registro;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistroResponseDTO(

        @Schema(
                description = "Identificador único do usuário",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Nome do usuário",
                example = "João Silva"
        )
        String nome,

        @Schema(
                description = "E-mail do usuário",
                example = "joao@email.com"
        )
        String email,

        @Schema(
                description = "Perfil de acesso do usuário",
                example = "USUARIO"
        )
        String perfil,

        @Schema(
                description = "Data e hora em que o usuário foi cadastrado",
                example = "2026-09-04T13:30:00"
        )
        LocalDateTime criadoEm
) { }
