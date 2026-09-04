package antifraud.servicotransacao.controller;

import antifraud.servicotransacao.dto.usuario.registro.CriarUsuarioAdminRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroResponseDTO;
import antifraud.servicotransacao.service.AdminUsuarioService;
import antifraud.servicotransacao.util.PaginaResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/usuarios")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Administração de Usuários",
        description = "Endpoints administrativos para gerenciamento de usuários"
)
public class AdminUsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    @PostMapping("/registro")
    @Operation(
            summary = "Cadastrar usuário",
            description = "Permite que um usuário com perfil ADMIN cadastre um novo usuário no sistema."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário cadastrado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não possui permissão de administrador"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "E-mail já cadastrado"
            )
    })
    public ResponseEntity<RegistroResponseDTO> registrarUsuario(
            @RequestBody @Valid CriarUsuarioAdminRequestDTO criarUsuarioAdminRequest) {

        log.info(
                "Tentativa de registro de usuário por admin para o e-mail: {}",
                criarUsuarioAdminRequest.email()
        );

        RegistroResponseDTO response =
                adminUsuarioService.registrarUsuarioPorAdmin(criarUsuarioAdminRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar usuário por ID",
            description = "Retorna os dados de um usuário específico a partir do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuário encontrado com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não possui permissão para acessar este recurso"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado"
            )
    })
    public ResponseEntity<RegistroResponseDTO> buscarUsuario(
            @Parameter(
                    description = "Identificador único do usuário",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable UUID id) {

        log.info("Tentativa de busca de usuário por ID: {}", id);

        RegistroResponseDTO response =
                adminUsuarioService.buscarUsuarioPorId(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(
            summary = "Listar usuários",
            description = "Retorna uma lista paginada de todos os usuários cadastrados."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuários listados com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Usuário não autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário não possui permissão de administrador"
            )
    })
    public ResponseEntity<PaginaResponseDTO<RegistroResponseDTO>> listarUsuarios(
            @Parameter(
                    description = "Número da página. A primeira página é 0.",
                    example = "0"
            )
            @RequestParam(defaultValue = "0") int pagina) {

        log.info("Tentativa de listagem de usuários");

        PaginaResponseDTO<RegistroResponseDTO> response =
                adminUsuarioService.listarTodosUsuarios(pagina);

        return ResponseEntity.ok(response);
    }
}
