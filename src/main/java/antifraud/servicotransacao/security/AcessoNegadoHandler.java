package antifraud.servicotransacao.security;

import antifraud.servicotransacao.dto.erros.ErroResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class AcessoNegadoHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErroResponseDTO erro = new ErroResponseDTO(
                403,
                "Acesso negado",
                "Você não tem permissão para acessar este recurso"
        );

        response.getWriter().write(objectMapper.writeValueAsString(erro));
    }
}
