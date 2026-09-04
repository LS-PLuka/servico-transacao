package antifraud.servicotransacao.controller;

import antifraud.servicotransacao.dto.transacao.TransacaoRequestDTO;
import antifraud.servicotransacao.dto.transacao.TransacaoResponseDTO;
import antifraud.servicotransacao.service.TransacaoService;
import antifraud.servicotransacao.util.PaginaResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/transacoes")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Transações",
        description = "Endpoints para registro e consulta de transações financeiras"
)
public class TransacaoController {

    private final TransacaoService transacaoService;

    @PostMapping("/efetuar")
    @Operation(
            summary = "Efetuar transação",
            description = "Registra uma nova transação e publica o evento para análise antifraude. " +
                    "Apenas usuários com perfil USUARIO podem registrar transações, e somente para a própria conta."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Transação registrada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados de entrada inválidos"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente ou inválido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Usuário ADMIN ou tentativa de registrar transação para outra conta"
            )
    })
    public ResponseEntity<TransacaoResponseDTO> efetuarTransacao(
            @RequestBody @Valid TransacaoRequestDTO transacaoRequest) {

        log.info("Tentativa de transaçao para conta com ID: {}", transacaoRequest.contaId());

        TransacaoResponseDTO response =
                transacaoService.efetuarTransacao(transacaoRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/conta/{contaId}")
    @Operation(
            summary = "Buscar transação por ID",
            description = "Retorna os dados de uma transação específica vinculada a uma conta. " +
                    "O usuário só pode consultar transações da própria conta, exceto perfis ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transação encontrada com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente ou inválido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Tentativa de acesso a transações de outra conta"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário ou transação não encontrada"
            )
    })
    public ResponseEntity<TransacaoResponseDTO> buscarTransacaoPorId(
            @PathVariable UUID id,
            @PathVariable UUID contaId) {

        log.info("Tentativa de busca de transação por ID: {} e contaId: {}", id, contaId);

        TransacaoResponseDTO response =
                transacaoService.buscarTransacaoPorId(id, contaId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/conta/{contaId}")
    @Operation(
            summary = "Listar transações de uma conta",
            description = "Retorna, de forma paginada, as transações de uma conta (10 registros por página). " +
                    "O usuário só pode listar transações da própria conta, exceto perfis ADMIN."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transações listadas com sucesso"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Token JWT ausente ou inválido"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Tentativa de acesso às transações de outra conta"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado"
            )
    })
    public ResponseEntity<PaginaResponseDTO<TransacaoResponseDTO>> listarTransacoesDeUmaConta(
            @PathVariable UUID contaId,
            @RequestParam(defaultValue = "0") int pagina) {

        log.info("Tentativa de listagem de transações para conta com ID: {}", contaId);

        PaginaResponseDTO<TransacaoResponseDTO> response =
                transacaoService.listarTransacoesDeUmaConta(contaId, pagina);

        return ResponseEntity.ok(response);
    }
}
