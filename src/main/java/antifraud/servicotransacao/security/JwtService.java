package antifraud.servicotransacao.security;

import antifraud.servicotransacao.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    //gerar e validar token
    public String gerarToken(UserDetails userDetails) {
        Usuario usuario = (Usuario) userDetails;

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("perfil", usuario.getPerfil().toString())
                .issuedAt(new Date())
                .expiration(new Date(
                        System.currentTimeMillis() + expiration)
                )
                .signWith(getSigningKey())
                .compact();
    }

    private boolean tokenExpirado(String token) {
        return new Date().after(extrairExpiracao(token));
    }

    public boolean tokenValido(String token, UserDetails userDetails) {
        String usernameToken = extrairUsername(token);
        String usernameUsuario = userDetails.getUsername();

        boolean usuario = usernameToken.equals(usernameUsuario);
        boolean tokenNaoExpirou = !tokenExpirado(token);

        return usuario && tokenNaoExpirou;
    }

    //metodos auxiliares para extrair informaçoes do token
    private Claims extrairTodasClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extrairUsername(String token) {
        Claims claims = extrairTodasClaims(token);
        return claims.getSubject();
    }

    public Date extrairExpiracao(String token) {
        Claims claims = extrairTodasClaims(token);
        return claims.getExpiration();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
