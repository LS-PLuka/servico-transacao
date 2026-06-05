package antifraud.servicotransacao.security;

import antifraud.servicotransacao.dto.erros.ErroResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class EntryPointNaoAutorizado implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErroResponseDTO erro = new ErroResponseDTO(
                401,
                "Não autorizado",
                "Token ausente ou inválido"
        );

        response.getWriter().write(objectMapper.writeValueAsString(erro));
    }
}
