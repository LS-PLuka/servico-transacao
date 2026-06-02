package antifraud.servicotransacao.service;

import antifraud.servicotransacao.dto.usuario.LoginRequestDTO;
import antifraud.servicotransacao.dto.usuario.LoginResponseDTO;
import antifraud.servicotransacao.entity.Usuario;
import antifraud.servicotransacao.repository.UsuarioRepository;
import antifraud.servicotransacao.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

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
