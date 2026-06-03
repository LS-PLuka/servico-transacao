package antifraud.servicotransacao.controller;

import antifraud.servicotransacao.dto.usuario.login.LoginRequestDTO;
import antifraud.servicotransacao.dto.usuario.login.LoginResponseDTO;
import antifraud.servicotransacao.dto.usuario.registro.CriarUsuarioAdminRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroResponseDTO;
import antifraud.servicotransacao.service.AutenticacaoUsuarioService;
import antifraud.servicotransacao.service.AdminUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthUsuarioController {

    private final AutenticacaoUsuarioService autenticacaoUsuarioService;

    @PostMapping("/registro")
    public ResponseEntity<RegistroResponseDTO> registro(@RequestBody @Valid RegistroRequestDTO registroRequest) {
        RegistroResponseDTO response = autenticacaoUsuarioService.registrarUsuario(registroRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO loginRequest) {
        LoginResponseDTO response = autenticacaoUsuarioService.autenticarUsuario(loginRequest);
        return ResponseEntity.ok(response);
    }
}
