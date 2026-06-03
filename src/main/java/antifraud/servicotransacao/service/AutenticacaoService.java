package antifraud.servicotransacao.service;

import antifraud.servicotransacao.dto.usuario.login.LoginRequestDTO;
import antifraud.servicotransacao.dto.usuario.login.LoginResponseDTO;
import antifraud.servicotransacao.entity.Usuario;
import antifraud.servicotransacao.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponseDTO autenticarUsuario(LoginRequestDTO loginRequest) {
        var authenticationToken = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.senha());
        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        Usuario usuario = (Usuario) authentication.getPrincipal();
        String token = jwtService.gerarToken(usuario);

        return new LoginResponseDTO(token, "Bearer", usuario.getPerfil().toString());
    }
}
