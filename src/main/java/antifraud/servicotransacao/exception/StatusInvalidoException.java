package antifraud.servicotransacao.exception;

public class StatusInvalidoException extends RuntimeException {
    public StatusInvalidoException(String mensagem) {
        super(mensagem);
    }
}
