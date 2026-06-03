package antifraud.servicotransacao.controller;

import antifraud.servicotransacao.dto.usuario.registro.CriarUsuarioAdminRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroResponseDTO;
import antifraud.servicotransacao.service.AdminUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.Mapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    @PostMapping("/registro")
    public ResponseEntity<RegistroResponseDTO> registrarUsuario(@RequestBody @Valid CriarUsuarioAdminRequestDTO criarUsuarioAdminRequest) {
        RegistroResponseDTO response = adminUsuarioService.registrarUsuarioPorAdmin(criarUsuarioAdminRequest);
        return ResponseEntity.ok(response);
    }
}
