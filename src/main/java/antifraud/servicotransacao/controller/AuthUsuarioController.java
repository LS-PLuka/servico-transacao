package antifraud.servicotransacao.controller;

import antifraud.servicotransacao.dto.usuario.login.LoginRequestDTO;
import antifraud.servicotransacao.dto.usuario.login.LoginResponseDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroResponseDTO;
import antifraud.servicotransacao.service.AutenticacaoUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Autenticação",
        description = "Endpoints para cadastro e autenticação de usuários"
)
public class AuthUsuarioController {

    private final AutenticacaoUsuarioService autenticacaoUsuarioService;

    @PostMapping("/registro")
    @Operation(
            summary = "Registrar usuário",
            description = "Realiza o cadastro de um novo usuário. O usuário registrado recebe automaticamente o perfil USUARIO."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário registrado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "E-mail já cadastrado"
            )
    })
    public ResponseEntity<RegistroResponseDTO> registro(
            @RequestBody @Valid RegistroRequestDTO registroRequest) {

        log.info("Tentativa de registro para o e-mail: {}", registroRequest.email());

        RegistroResponseDTO response =
                autenticacaoUsuarioService.registrarUsuario(registroRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Autenticar usuário",
            description = "Autentica um usuário utilizando e-mail e senha e retorna um token JWT."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário autenticado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "E-mail ou senha inválidos"
            )
    })
    public ResponseEntity<LoginResponseDTO> login(
            @RequestBody @Valid LoginRequestDTO loginRequest) {

        log.info("Tentativa de login para o e-mail: {}", loginRequest.email());

        LoginResponseDTO response =
                autenticacaoUsuarioService.autenticarUsuario(loginRequest);

        return ResponseEntity.ok(response);
    }
}
