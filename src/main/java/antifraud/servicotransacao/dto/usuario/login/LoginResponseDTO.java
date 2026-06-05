package antifraud.servicotransacao.dto.usuario.login;

public record LoginResponseDTO(
    String token,
    String tipo,
    String perfil
) { }
