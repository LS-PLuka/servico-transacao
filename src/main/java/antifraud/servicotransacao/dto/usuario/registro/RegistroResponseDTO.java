package antifraud.servicotransacao.dto.usuario.registro;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistroResponseDTO(
    UUID id,
    String nome,
    String email,
    String perfil,
    LocalDateTime criadoEm
) { }
