package antifraud.servicotransacao.messaging;

import antifraud.servicotransacao.config.RabbitMQConfig;
import antifraud.servicotransacao.dto.transacao.TransacaoEventoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransacaoPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicarTransacao(TransacaoEventoDTO evento) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_TRANSACOES,
                RabbitMQConfig.ROUTING_KEY_RISCO,
                evento
        );

        log.info(
                "Evento publicado no RabbitMQ: Transacao={}, Conta={}",
                evento.transacaoId(),
                evento.contaId()
        );
    }
}
