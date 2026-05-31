package antifraud.servicotransacao.dto.usuario;

public record LoginResponseDTO(
    String token,
    String tipo,
    String perfil
) { }
