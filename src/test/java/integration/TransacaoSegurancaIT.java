package integration;

import antifraud.servicotransacao.dto.transacao.TransacaoRequestDTO;
import antifraud.servicotransacao.dto.usuario.login.LoginRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroRequestDTO;
import antifraud.servicotransacao.repository.TransacaoRepository;
import antifraud.servicotransacao.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IT: regras de acesso das transacoes")
class TransacaoSegurancaIT extends IntegracaoBaseTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EMAIL_A = "usuario.a@teste.com";
    private static final String EMAIL_B = "usuario.b@teste.com";
    private static final String SENHA = "senha123";

    @BeforeEach
    void limparEstado() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve retornar 401 ao efetuar transacao sem token")
    void efetuarTransacao_semToken_deveRetornar401() {
        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/transacoes/efetuar",
                new TransacaoRequestDTO(
                        UUID.randomUUID(),
                        new BigDecimal("10.00"),
                        "MERCADO",
                        "BRA",
                        LocalDateTime.now()),
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(transacaoRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deve retornar 403 ao efetuar transacao para outra conta")
    void efetuarTransacao_outraConta_deveRetornar403() throws Exception {
        registrar(EMAIL_A, SENHA);
        UUID contaB = registrar(EMAIL_B, SENHA);
        String tokenA = autenticar(EMAIL_A, SENHA);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/transacoes/efetuar",
                HttpMethod.POST,
                new HttpEntity<>(new TransacaoRequestDTO(
                        contaB,
                        new BigDecimal("99.90"),
                        "MERCADO",
                        "BRA",
                        LocalDateTime.now()),
                        headersComToken(tokenA)),
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(transacaoRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deve retornar 403 ao listar transacoes de outra conta")
    void listarTransacoes_outraConta_deveRetornar403() throws Exception {
        registrar(EMAIL_A, SENHA);
        UUID contaB = registrar(EMAIL_B, SENHA);
        String tokenA = autenticar(EMAIL_A, SENHA);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/transacoes/conta/{contaId}",
                HttpMethod.GET,
                new HttpEntity<>(headersComToken(tokenA)),
                String.class,
                contaB);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Deve retornar 404 ao buscar transacao inexistente")
    void buscarTransacao_inexistente_deveRetornar404() throws Exception {
        UUID contaId = registrar(EMAIL_A, SENHA);
        String token = autenticar(EMAIL_A, SENHA);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/transacoes/{id}/conta/{contaId}",
                HttpMethod.GET,
                new HttpEntity<>(headersComToken(token)),
                String.class,
                UUID.randomUUID(), contaId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Deve retornar 400 quando o codigo do pais e invalido")
    void efetuarTransacao_codigoPaisInvalido_deveRetornar400() throws Exception {
        UUID contaId = registrar(EMAIL_A, SENHA);
        String token = autenticar(EMAIL_A, SENHA);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/transacoes/efetuar",
                HttpMethod.POST,
                new HttpEntity<>(new TransacaoRequestDTO(
                        contaId,
                        new BigDecimal("10.00"),
                        "MERCADO",
                        "brasil",
                        LocalDateTime.now()),
                        headersComToken(token)),
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(transacaoRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deve retornar 400 quando o valor e menor ou igual a zero")
    void efetuarTransacao_valorInvalido_deveRetornar400() throws Exception {
        UUID contaId = registrar(EMAIL_A, SENHA);
        String token = autenticar(EMAIL_A, SENHA);

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/transacoes/efetuar",
                HttpMethod.POST,
                new HttpEntity<>(new TransacaoRequestDTO(
                        contaId,
                        new BigDecimal("0.00"),
                        "MERCADO",
                        "BRA",
                        LocalDateTime.now()),
                        headersComToken(token)),
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(transacaoRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deve retornar 409 ao registrar email ja cadastrado")
    void registrar_emailDuplicado_deveRetornar409() throws Exception {
        registrar(EMAIL_A, SENHA);

        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/auth/registro",
                new RegistroRequestDTO("Outro Usuario", EMAIL_A, SENHA),
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("Deve retornar 401 ao autenticar com senha invalida")
    void autenticar_senhaInvalida_deveRetornar401() throws Exception {
        registrar(EMAIL_A, SENHA);

        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/auth/login",
                new LoginRequestDTO(EMAIL_A, "senhaErrada"),
                String.class);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private UUID registrar(String email, String senha) throws Exception {
        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/auth/registro",
                new RegistroRequestDTO("Usuario " + email, email, senha),
                String.class);

        return UUID.fromString(
                objectMapper.readTree(resposta.getBody()).get("id").asText());
    }

    private String autenticar(String email, String senha) throws Exception {
        ResponseEntity<String> resposta = restTemplate.postForEntity(
                "/auth/login",
                new LoginRequestDTO(email, senha),
                String.class);

        return objectMapper.readTree(resposta.getBody()).get("token").asText();
    }

    private HttpHeaders headersComToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}
