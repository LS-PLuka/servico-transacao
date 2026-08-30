package antifraud.servicotransacao.controller;

import antifraud.servicotransacao.dto.transacao.TransacaoRequestDTO;
import antifraud.servicotransacao.dto.transacao.TransacaoResponseDTO;
import antifraud.servicotransacao.service.TransacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transacoes")
@RequiredArgsConstructor
@Slf4j
public class TransacaoController {

    private final TransacaoService transacaoService;

    @PostMapping("/efetuar")
    public ResponseEntity<TransacaoResponseDTO> efetuarTransacao(@RequestBody @Valid TransacaoRequestDTO transacaoRequest) {
        log.info("Tentativa de transaçao para conta com ID: {}", transacaoRequest.contaId());

        TransacaoResponseDTO response = transacaoService.registrarTransacao(transacaoRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
