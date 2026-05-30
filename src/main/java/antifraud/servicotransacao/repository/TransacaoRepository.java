package antifraud.servicotransacao.repository;

import antifraud.servicotransacao.entity.Transacao;
import antifraud.servicotransacao.enums.StatusTransacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransacaoRepository extends JpaRepository<Transacao, UUID> {

    List<Transacao> findByContaId(UUID contaId);

    List<Transacao> findByContaIdAndStatus(UUID contaId, StatusTransacao status);
}
