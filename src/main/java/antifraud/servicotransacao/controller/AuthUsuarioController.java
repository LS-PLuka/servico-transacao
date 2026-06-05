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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthUsuarioController {

    private final AutenticacaoUsuarioService autenticacaoUsuarioService;

    @PostMapping("/registro")
    public ResponseEntity<RegistroResponseDTO> registro(@RequestBody @Valid RegistroRequestDTO registroRequest) {
        log.info("Tentativa de registro para o e-mail: {}", registroRequest.email());

        RegistroResponseDTO response = autenticacaoUsuarioService.registrarUsuario(registroRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO loginRequest) {
        log.info("Tentativa de login para o e-mail: {}", loginRequest.email());

        LoginResponseDTO response = autenticacaoUsuarioService.autenticarUsuario(loginRequest);
        return ResponseEntity.ok(response);
    }
}
