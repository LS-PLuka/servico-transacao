package antifraud.servicotransacao.dto.erros;

public record ErroResponseDTO(
    int status,
    String erro,
    String mensagem
) { }
