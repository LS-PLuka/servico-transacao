package antifraud.servicotransacao.config;

import antifraud.servicotransacao.dto.transacao.TransacaoEventoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RabbitMQConfig.class)
            .withBean(ObjectMapper.class, () -> new ObjectMapper()
                    .findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));

    @Test
    @DisplayName("Deve serializar as datas do evento no formato ISO-8601")
    void messageConverter_eventoComDatas_deveSerializarDatasComoTexto() {
        LocalDateTime dataHora = LocalDateTime.of(2026, 9, 24, 10, 30, 15);
        LocalDateTime contaCriadaEm = LocalDateTime.of(2026, 9, 20, 8, 10, 5);
        TransacaoEventoDTO evento = new TransacaoEventoDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("150.75"),
                "RESTAURANTE",
                "BRA",
                dataHora,
                contaCriadaEm
        );

        contextRunner.run(context -> {
            Jackson2JsonMessageConverter converter = context.getBean(Jackson2JsonMessageConverter.class);
            ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

            Message mensagem = converter.toMessage(evento, new MessageProperties());
            JsonNode payload = objectMapper.readTree(mensagem.getBody());

            assertThat(payload.get("dataHora").isTextual()).isTrue();
            assertThat(LocalDateTime.parse(payload.get("dataHora").asText())).isEqualTo(dataHora);
            assertThat(payload.get("contaCriadaEm").isTextual()).isTrue();
            assertThat(LocalDateTime.parse(payload.get("contaCriadaEm").asText())).isEqualTo(contaCriadaEm);
        });
    }
}
