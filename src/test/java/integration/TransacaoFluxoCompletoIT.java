package integration;

import antifraud.servicotransacao.config.RabbitMQConfig;
import antifraud.servicotransacao.dto.transacao.TransacaoRequestDTO;
import antifraud.servicotransacao.dto.usuario.login.LoginRequestDTO;
import antifraud.servicotransacao.dto.usuario.registro.RegistroRequestDTO;
import antifraud.servicotransacao.entity.Transacao;
import antifraud.servicotransacao.enums.StatusTransacao;
import antifraud.servicotransacao.repository.TransacaoRepository;
import antifraud.servicotransacao.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@DisplayName("IT: login -> transacao -> persistencia -> fila -> consulta")
class TransacaoFluxoCompletoIT extends IntegracaoBaseTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransacaoRepository transacaoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EMAIL = "integracao@teste.com";
    private static final String SENHA = "senha123";

    @BeforeEach
    void limparEstado() {
        transacaoRepository.deleteAll();
        usuarioRepository.deleteAll();
        drenarFila();
    }

    @Test
    @DisplayName("Deve registrar, autenticar, publicar evento e consultar a transacao")
    void fluxoCompleto_dadosValidos_devePersistirEPublicarEvento() throws Exception {
        ResponseEntity<String> respRegistro = restTemplate.postForEntity(
                "/auth/registro",
                new RegistroRequestDTO("Usuario Integracao", EMAIL, SENHA),
                String.class);

        assertThat(respRegistro.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        JsonNode corpoRegistro = objectMapper.readTree(respRegistro.getBody());
        UUID contaId = UUID.fromString(corpoRegistro.get("id").asText());

        assertThat(corpoRegistro.get("email").asText()).isEqualTo(EMAIL);
        assertThat(corpoRegistro.get("perfil").asText()).isEqualTo("USUARIO");

        ResponseEntity<String> respLogin = restTemplate.postForEntity(
                "/auth/login",
                new LoginRequestDTO(EMAIL, SENHA),
                String.class);

        assertThat(respLogin.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode corpoLogin = objectMapper.readTree(respLogin.getBody());
        String token = corpoLogin.get("token").asText();

        assertThat(token).isNotBlank();
        assertThat(corpoLogin.get("tipo").asText()).isEqualTo("Bearer");
        assertThat(corpoLogin.get("perfil").asText()).isEqualTo("USUARIO");

        TransacaoRequestDTO requestTransacao = new TransacaoRequestDTO(
                contaId,
                new BigDecimal("150.75"),
                "RESTAURANTE",
                "BRA",
                LocalDateTime.now());

        ResponseEntity<String> respTransacao = restTemplate.exchange(
                "/transacoes/efetuar",
                HttpMethod.POST,
                new HttpEntity<>(requestTransacao, headersComToken(token)),
                String.class);

        assertThat(respTransacao.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UUID transacaoId = UUID.fromString(
                objectMapper.readTree(respTransacao.getBody()).get("id").asText());

        List<Transacao> transacoes = transacaoRepository.findAll();
        assertThat(transacoes).hasSize(1);

        Transacao salva = transacoes.get(0);
        assertThat(salva.getId()).isEqualTo(transacaoId);
        assertThat(salva.getContaId()).isEqualTo(contaId);
        assertThat(salva.getValor()).isEqualByComparingTo("150.75");
        assertThat(salva.getCategoria()).isEqualTo("RESTAURANTE");
        assertThat(salva.getCodigoPais()).isEqualTo("BRA");
        assertThat(salva.getStatus()).isEqualTo(StatusTransacao.PENDENTE);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Message mensagem = rabbitTemplate.receive(RabbitMQConfig.FILA_ANALISE);

            assertThat(mensagem)
                    .as("nenhuma mensagem na fila %s", RabbitMQConfig.FILA_ANALISE)
                    .isNotNull();

            JsonNode evento = objectMapper.readTree(mensagem.getBody());

            assertThat(UUID.fromString(evento.get("id").asText())).isEqualTo(transacaoId);
            assertThat(UUID.fromString(evento.get("contaId").asText())).isEqualTo(contaId);
            assertThat(new BigDecimal(evento.get("valor").asText()))
                    .isEqualByComparingTo("150.75");
        });

        ResponseEntity<String> respBusca = restTemplate.exchange(
                "/transacoes/{id}/conta/{contaId}",
                HttpMethod.GET,
                new HttpEntity<>(headersComToken(token)),
                String.class,
                transacaoId, contaId);

        assertThat(respBusca.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode corpoBusca = objectMapper.readTree(respBusca.getBody());

        assertThat(UUID.fromString(corpoBusca.get("id").asText())).isEqualTo(transacaoId);
        assertThat(UUID.fromString(corpoBusca.get("contaId").asText())).isEqualTo(contaId);
        assertThat(new BigDecimal(corpoBusca.get("valor").asText()))
                .isEqualByComparingTo("150.75");
        assertThat(corpoBusca.get("categoria").asText()).isEqualTo("RESTAURANTE");
        assertThat(corpoBusca.get("codigoPais").asText()).isEqualTo("BRA");
        assertThat(corpoBusca.get("status").asText()).isEqualTo("PENDENTE");
    }

    @Test
    @DisplayName("Deve listar transacoes da propria conta de forma paginada")
    void listarTransacoes_contaPropria_deveRetornarPagina() throws Exception {
        UUID contaId = registrar(EMAIL, SENHA);
        String token = autenticar(EMAIL, SENHA);

        for (int i = 1; i <= 3; i++) {
            ResponseEntity<String> resposta = restTemplate.exchange(
                    "/transacoes/efetuar",
                    HttpMethod.POST,
                    new HttpEntity<>(new TransacaoRequestDTO(
                            contaId,
                            new BigDecimal(i + ".00"),
                            "MERCADO",
                            "BRA",
                            LocalDateTime.now()),
                            headersComToken(token)),
                    String.class);

            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }

        ResponseEntity<String> resposta = restTemplate.exchange(
                "/transacoes/conta/{contaId}?pagina=0",
                HttpMethod.GET,
                new HttpEntity<>(headersComToken(token)),
                String.class,
                contaId);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode corpo = objectMapper.readTree(resposta.getBody());

        assertThat(corpo.get("totalItens").asInt()).isEqualTo(3);
        assertThat(corpo.get("conteudo")).hasSize(3);
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

    private void drenarFila() {
        try {
            while (rabbitTemplate.receive(RabbitMQConfig.FILA_ANALISE, 100) != null) {
            }
        } catch (AmqpException ignored) {
        }
    }
}
