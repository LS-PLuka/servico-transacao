package antifraud.servicotransacao.controller;

import antifraud.servicotransacao.dto.usuario.registro.CriarUsuarioAdminRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroResponseDTO;
import antifraud.servicotransacao.service.AdminUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
@Slf4j
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    @PostMapping("/registro")
    public ResponseEntity<RegistroResponseDTO> registrarUsuario(@RequestBody @Valid CriarUsuarioAdminRequestDTO criarUsuarioAdminRequest) {
        log.info("Tentativa de registro de usuário por admin para o e-mail: {}", criarUsuarioAdminRequest.email());

        RegistroResponseDTO response = adminUsuarioService.registrarUsuarioPorAdmin(criarUsuarioAdminRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
