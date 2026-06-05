package antifraud.servicotransacao.exception;

import antifraud.servicotransacao.dto.erros.ErroResponseDTO;
import antifraud.servicotransacao.dto.erros.ErroValidacaoResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResponseDTO> handleEmailJaCadastrado(EmailJaCadastradoException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErroResponseDTO(409, "Conflito", ex.getMessage()));
    }

    @ExceptionHandler(TransacaoNaoEncontradaException.class)
    public ResponseEntity<ErroResponseDTO> handleTransacaoNaoEncontrada(TransacaoNaoEncontradaException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErroResponseDTO(404, "Não encontrado", ex.getMessage()));
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ErroResponseDTO> handleUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErroResponseDTO(404, "Não encontrado", ex.getMessage()));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponseDTO> handleAcessoNegado(AcessoNegadoException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErroResponseDTO(403, "Acesso negado", ex.getMessage()));
    }

    @ExceptionHandler(StatusInvalidoException.class)
    public ResponseEntity<ErroResponseDTO> handleStatusInvalido(StatusInvalidoException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponseDTO(422, "Status inválido", ex.getMessage()));
    }

    //lancado pelo spring quando uma validacao do DTO falha - @NotBlank, @Email, etc
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroValidacaoResponseDTO> handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.put(erro.getField(), erro.getDefaultMessage()));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErroValidacaoResponseDTO(400, "Erro de validação", erros));
    }

    //qualquer excecao nao tratada cai aqui
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> handleErroGenerico(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroResponseDTO(500, "Erro interno", "Ocorreu um erro inesperado"));
    }
}
