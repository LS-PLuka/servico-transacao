package antifraud.servicotransacao.controller;

import antifraud.servicotransacao.dto.transacao.TransacaoRequestDTO;
import antifraud.servicotransacao.dto.transacao.TransacaoResponseDTO;
import antifraud.servicotransacao.service.TransacaoService;
import antifraud.servicotransacao.util.PaginaResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/transacoes")
@RequiredArgsConstructor
@Slf4j
public class TransacaoController {

    private final TransacaoService transacaoService;

    @PostMapping("/efetuar")
    public ResponseEntity<TransacaoResponseDTO> efetuarTransacao(@RequestBody @Valid TransacaoRequestDTO transacaoRequest) {
        log.info("Tentativa de transaçao para conta com ID: {}", transacaoRequest.contaId());

        TransacaoResponseDTO response = transacaoService.efetuarTransacao(transacaoRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/conta/{contaId}")
    public ResponseEntity<TransacaoResponseDTO> buscarTransacaoPorId(@PathVariable UUID id, @PathVariable UUID contaId) {
        log.info("Tentativa de busca de transação por ID: {} e contaId: {}", id, contaId);

        TransacaoResponseDTO response = transacaoService.buscarTransacaoPorId(id, contaId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conta/{contaId}")
    public ResponseEntity<PaginaResponseDTO<TransacaoResponseDTO>> listarTransacoesDeUmaConta(@PathVariable UUID contaId, @RequestParam(defaultValue = "0") int pagina) {
        log.info("Tentativa de listagem de transações para conta com ID: {}", contaId);

        PaginaResponseDTO<TransacaoResponseDTO> response = transacaoService.listarTransacoesDeUmaConta(contaId, pagina);
        return ResponseEntity.ok(response);
    }
}
