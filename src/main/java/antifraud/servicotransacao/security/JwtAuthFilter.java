package antifraud.servicotransacao.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        // verifica se o token veio na request e se inicia com Bearer
        // se ele nao vier ou nao iniciar com Bearer, continua o fluxo normal
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // se o token vier, extrai o username e o token (sem o Bearer)
            String token = authHeader.substring(7);
            String username = jwtService.extrairUsername(token);

            // se nao estiver autenticado entra na logica de autenticar
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = usuarioDetailsService.loadUserByUsername(username);

                if (jwtService.tokenValido(token, userDetails)) {
                    // implementaçao da interface Authentication - representa a autenticaçao do usuario
                    // (Principal (usuario), Credentials (senha), Authorities (permissoes))
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );

                    // contexto do Spring para autenticar o usuario - SecurityContextHolder
                    // setAuthentication recebe authToken e passa a ser autenticado
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Usuario autenticado com sucesso: " + username);
                }
            }
        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            logger.warn("Token expirado");
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            logger.warn("Token inválido");
        }

        filterChain.doFilter(request, response);
    }
}
