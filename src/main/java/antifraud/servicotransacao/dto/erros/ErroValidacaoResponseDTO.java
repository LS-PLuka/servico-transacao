package antifraud.servicotransacao.dto.erros;

import java.util.Map;

public record ErroValidacaoResponseDTO(
    int status,
    String erro,
    Map<String, String> erros
) { }
